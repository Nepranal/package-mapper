package com.mizookie.packagemapper.controllers;

import com.mizookie.packagemapper.dto.user.DependencyGraphResponse;
import com.mizookie.packagemapper.services.AnalyserService;
import com.mizookie.packagemapper.services.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin()
@RequestMapping("/analyse")
public class AnalyserController {
    private final AnalyserService analyserService;
    private final GraphService graphService;
    @Value("${repository.directory}")
    private String localRepositoryDirectory;

    @Autowired
    public AnalyserController(AnalyserService analyserService, GraphService graphService) {
        this.analyserService = analyserService;
        this.graphService = graphService;
    }

    /**
     * This method receives a repository path and triggers the analysis of the code in the repository.
     *
     * @param requestBody The request body containing the repository path.
     */
    @PostMapping("/custom")
    public void analyse(@RequestBody Map<String, String> requestBody) throws IOException, GitAPIException, InterruptedException {
        String repositoryPath = requestBody.get("repositoryPath");
        String version = requestBody.get("version");
        log.info("Repository path received: {}", repositoryPath);
        analyserService.analyse(repositoryPath, version);
    }

    /**
     * This method triggers the analysis of the code in all repositories.
     */
    @PostMapping("/all")
    public void analyseAll() {
        log.info("Analyzing all repositories...");
        try {
            analyserService.analyse();
        } catch (IOException | GitAPIException | InterruptedException e) {
            log.error("Failed to analyze all repositories: {}", e.getMessage());
        }
    }

    /**
     * This method triggers the visualization of demo data.
     */
    @PostMapping("/visualize-demo")
    public void visualize() {
        log.info("Visualizing the parsed data...");
        analyserService.visualizeDemo();
    }

    @GetMapping("/graph")
    public List<DependencyGraphResponse> generateGraph(@RequestParam String repo, @RequestParam String version) throws GitAPIException, IOException, InterruptedException {
        ArrayList<DependencyGraphResponse> responses = new ArrayList<>();
        Graph<String, DefaultEdge> graph;
        try {
            graph = graphService.importGraph(String.format("%s_%s", repo, version));
        } catch (FileNotFoundException e) {
            analyserService.analyse(String.format("%s/%s", localRepositoryDirectory, repo), version);
            graph = graphService.importGraph(String.format("%s_%s", repo, version));
        }
        Graph<String, DefaultEdge> finalGraph = graph;
        finalGraph.edgeSet().forEach(e -> {
            responses.add(new DependencyGraphResponse(finalGraph.getEdgeSource(e), finalGraph.getEdgeTarget(e)));
        });
        return responses;
    }
}
