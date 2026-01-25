package com.mizookie.packagemapper.services.implementations;

import com.mizookie.packagemapper.resolver.NaiveResolver;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
public class AnalyserServiceImpl implements AnalyserService {

    private static int N;
    private final GraphService graphService;
    private final GithubRepositoryService githubRepositoryService;
    private final ArrayList<AnalyzerTask> threads = new ArrayList<>();
    @Value("${repository.directory}")
    private String localRepositoryDirectory;

    @Autowired
    public AnalyserServiceImpl(GraphService graphService, GithubRepositoryService githubRepositoryService, @Value("${threads.num}") String n) {
        this.graphService = graphService;
        this.githubRepositoryService = githubRepositoryService;
        N = Integer.parseInt(n);

        for (int i = 0; i < N; ++i) {
            AnalyzerTask t = new AnalyzerTask(i);
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

        AnalyzerTask.readerSemaphore.acquire(N);
        graphService.setDependencyMap(new HashMap<>());
        AnalyzerTask.filePaths = FileService.getFiles(repositoryPath);
        int numberOfFiles = AnalyzerTask.filePaths.size();
        int division = (int) Math.ceil(numberOfFiles / (1.0 * N));
        for (int i = 0; i < N; ++i) {
            AnalyzerTask t = threads.get(i);
            t.setStartPoint(i * division);
            t.setEndPoint((division == 1) ?
                    Math.min(numberOfFiles + 1, i + 1) % (numberOfFiles + 1) - 1
                    : Math.min(numberOfFiles, (i + 1) * division) - 1);
        }
        AnalyzerTask.producerSemaphore.release(N);

        // Wait until all finished
        AnalyzerTask.readerSemaphore.acquire(N);
        for (int i = 0; i < N; ++i) {
            AnalyzerTask t = threads.get(i);
            t.setStartPoint(0);
            t.setEndPoint(-1);
        }
        AnalyzerTask.producerSemaphore.release(N);

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

    class AnalyzerTask extends Thread {
        static Semaphore naiveResolverLock = new Semaphore(1);
        static Semaphore resultLock = new Semaphore(1);
        static Semaphore readerSemaphore = new Semaphore(0);
        static Semaphore producerSemaphore = new Semaphore(N);
        static List<String> filePaths;
        private static NaiveResolver naiveResolver = new NaiveResolver(3);
        private int startPoint, endPoint, id;

        AnalyzerTask(int id) {
            this.id = id;
        }

        public void run() {
            try {
                System.out.println("Analyzer resolver threads active");
                while (true) {
                    readerSemaphore.acquire();
                    for (int i = startPoint; i <= endPoint; ++i) {
                        String filePath = filePaths.get(i);
                        List<String> results;
                        switch (FileService.getFileExtension(filePath)) {
                            case ".py":
                            case ".java":
                            default:
                                naiveResolverLock.acquire();
                                results = naiveResolver.solve(filePaths, filePath);
                                naiveResolverLock.release();
                        }
                        resultLock.acquire();
                        for (String result : results) {
                            if (!result.equals(filePath)) {
                                graphService.addEdge(FileService.getFileNameWithExtension(result),
                                        FileService.getFileNameWithExtension(filePath));
                            }
                        }
                        resultLock.release();
                    }
                    producerSemaphore.release();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void setStartPoint(int startPoint) {
            this.startPoint = startPoint;
        }

        public void setEndPoint(int endPoint) {
            this.endPoint = endPoint;
        }
    }

}
