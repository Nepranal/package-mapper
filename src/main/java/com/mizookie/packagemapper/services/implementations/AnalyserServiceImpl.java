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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AnalyserServiceImpl implements AnalyserService {

    private final GraphService graphService;
    private final GithubRepositoryService githubRepositoryService;
    @Value("${repository.directory}")
    private String localRepositoryDirectory;
    private Map<String, List<String>> classesMap = new HashMap<>();

    @Autowired
    public AnalyserServiceImpl(GraphService graphService, GithubRepositoryService githubRepositoryService) {
        this.graphService = graphService;
        this.githubRepositoryService = githubRepositoryService;
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
    @Override
    public void analyse(String repositoryPath, String version) throws IOException, GitAPIException, InterruptedException {
        List<String> repoFiles = FileService.getFiles(repositoryPath);
        String repositoryName = FileService.getFileNameOnly(repositoryPath);
        int N = repoFiles.size();

        if (version == null) {
            version = githubRepositoryService.getCurrentCommit(repositoryName);
        }
        githubRepositoryService.checkoutCommit(repositoryName, version);

        graphService.setDependencyMap(classesMap);
        for (int i = 0; i < N - 1; ++i) {
            for (int j = i + 1; j < N; ++j) {
                String path1 = repoFiles.get(i);
                String path2 = repoFiles.get(j);
                if (findInCrawl(path1, new String[]{FileService.getFileNameWithoutExtension(path2)})) {
                    graphService.addEdge(path1, path2);
                }
                if (findInCrawl(path2, new String[]{FileService.getFileNameWithoutExtension(path1)})) {
                    graphService.addEdge(path2, path1);
                }
            }
        }
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

    // This method crawls the code in the repository and extracts the imports
    private Boolean findInCrawl(String path, String[] targets) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path));) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches(String.format(".*\\b(%s)\\b.*", String.join("|", targets)))) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println(path);
        }
        return false;
    }
}
