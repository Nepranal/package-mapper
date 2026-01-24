package com.mizookie.packagemapper.services.implementations;

import com.mizookie.packagemapper.services.AnalyserService;
import com.mizookie.packagemapper.services.GithubRepositoryService;
import com.mizookie.packagemapper.services.GraphService;
import com.mizookie.packagemapper.utils.FileService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class AnalyserServiceImpl implements AnalyserService {
    static private int N;
    private final GraphService graphService;
    private final GithubRepositoryService githubRepositoryService;
    private final ArrayList<AnalyserTask> threads = new ArrayList<>();
    @Value("${repository.directory}")
    private String localRepositoryDirectory;

    @Autowired
    public AnalyserServiceImpl(GraphService graphService, GithubRepositoryService githubRepositoryService, @Value("${threads.num}") String n) {
        this.graphService = graphService;
        this.githubRepositoryService = githubRepositoryService;
        N = Integer.parseInt(n);

        for (int i = 0; i < N; ++i) {
            AnalyserTask t = new AnalyserTask(i);
            threads.add(t);
            t.start();
        }
    }

    /**
     * This method visualizes the parsed data in a graphical format.
     *
     * @param classesMap The map containing the parsed data.
     */
    @Override
    public void visualize(Map<String, List<String>> classesMap) {
        // Improve visualization of the parsed data in a graphical format
        log.info("Visualizing the parsed data...");

        // Generate the visualization
        graphService.displayGraph(FileService.getFileNameOnly(localRepositoryDirectory));
    }

    /**
     * This method orchestrates the crawling, parsing and visualization of the code
     * in the repository.
     *
     * @param repositoryPath The path to the repository to analyze.
     */
    // This is a producer for AnalyserTask
    @Override
    public void analyse(String repositoryPath, String version) throws IOException, GitAPIException, InterruptedException {
        String repositoryName = FileService.getFileNameOnly(repositoryPath);
        if (version == null) {
            version = githubRepositoryService.getCurrentCommit(repositoryName);
        }
        githubRepositoryService.checkoutCommit(repositoryName, version);

        // Initialise the threads
        AnalyserTask.fileNames = FileService.getFiles(repositoryPath);
        int numberOfFiles = AnalyserTask.fileNames.size();
        int division = (int) Math.ceil(numberOfFiles / (1.0 * N));
        for (int i = 0; i < N; ++i) {
            AnalyserTask t = threads.get(i);
            t.setStartPoint(i * division);
            t.setEndPoint((division == 1) ?
                    Math.min(numberOfFiles + 1, i + 1) % (numberOfFiles + 1) - 1
                    : Math.min(numberOfFiles, (i + 1) * division) - 1);
//            System.out.printf("%d %d%n", t.startPoint, t.endPoint);
        }
        graphService.setDependencyMap(AnalyserTask.classesMap);

        for (String filePath : AnalyserTask.fileNames) {
            // Producer stuff here
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    AnalyserTask.producerSemaphore.acquire(N);
                    AnalyserTask.currentFileName = filePath;
                    AnalyserTask.line = line;
                    AnalyserTask.readerSemaphore.release(N);
                }
            }
        }
        AnalyserTask.line = "";
        AnalyserTask.currentFileName = "";
        AnalyserTask.readerSemaphore.release(N);
        AnalyserTask.producerSemaphore.acquire(N);

        graphService.serializeGraph(repositoryName, version);
    }

    /**
     * This method analyzes all repositories.
     */
    @Override
    public void analyse() throws IOException, GitAPIException, InterruptedException {
        // Analyze all local repositories
        log.info("Analyzing all local repositories...");

        // Crawl through all repositories within the local repository directory
        List<String> repositories = FileService.getDirectories(localRepositoryDirectory);

        for (String repository : repositories) {
            analyse(repository, null); // TODO: This function should take a collection of version
        }
    }

    @Override
    public void visualizeDemo() {
        // Visualize the parsed data
        Graph<String, DefaultEdge> graph = GraphServiceImpl.createMediumGraph();
        graphService.setDependencyMap(graph);
        graphService.displayGraph("test");
    }

    // Reader - see analyse()
    class AnalyserTask extends Thread {
        static final Map<String, List<String>> classesMap = new HashMap<>();
        static List<String> fileNames;
        static String currentFileName;
        static Semaphore readerSemaphore = new Semaphore(0);
        static Semaphore producerSemaphore = new Semaphore(N);
        static Semaphore resultLock = new Semaphore(1);
        static Lock doneProcessingLock = new ReentrantLock();
        static String line;
        static int[] doneProcessing = {0, 0};
        static Condition[] doneProcessingConditions = {doneProcessingLock.newCondition(), doneProcessingLock.newCondition()};
        ArrayList<Integer> range = new ArrayList<>();
        private int id;
        private int startPoint, endPoint, found = 0, round = 1;

        AnalyserTask(int id) {
            this.id = id;
        }

        public void run() {
            String line, currentFileName = "";
            while (true) {
                try {
                    readerSemaphore.acquire();
                    // New line available
                    if (!currentFileName.equals(AnalyserTask.currentFileName)) {
                        initState();
                    }
                    currentFileName = AnalyserTask.currentFileName; // Copy current file name
                    line = AnalyserTask.line; // Copy line
                    producerSemaphore.release();
                    processLine(line, currentFileName);

                    doneProcessingLock.lock();
                    doneProcessing[round] += 1;
                    while (doneProcessing[round] < N) {
                        doneProcessingConditions[round].await();
                        doneProcessing[round] = N;
                    }
                    doneProcessingConditions[round].signalAll();
                    doneProcessing[round] = 0;
                    doneProcessingLock.unlock();
                    round = (round + 1) % 2;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void initState() {
            found = 0;
            range = new ArrayList<>();
            for (int i = startPoint; i <= endPoint; ++i) {
                range.add(i);
            }
        }

        private void processLine(String line, String currentFileName) throws InterruptedException {
            ArrayList<String> temp = new ArrayList<>();
            for (int i = found; i < range.size(); ++i) {
                String target = FileService.getFileNameWithoutExtension(fileNames.get(range.get(i)));
                if (line.matches(String.format(".*\\b(%s)\\b.*", target))) {
                    temp.add(fileNames.get(range.get(i)));
                    Collections.swap(range, found, i);
                    found += 1;
                }
            }

            resultLock.acquire();
            for (String s : temp) {
                if (!s.equals(currentFileName)) {
                    graphService.addEdge(FileService.getFileNameWithExtension(s),
                            FileService.getFileNameWithExtension(currentFileName));
                }
            }
            resultLock.release();
        }

        public void setStartPoint(int startPoint) {
            this.startPoint = startPoint;
        }

        public void setEndPoint(int endPoint) {
            this.endPoint = endPoint;
        }
    }
}
