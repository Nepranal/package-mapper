package com.mizookie.packagemapper.controllers;

import com.mizookie.packagemapper.services.AnalyserService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/analyse")
public class AnalyserController {
    private final AnalyserService analyserService;

    @Autowired
    public AnalyserController(AnalyserService analyserService) {
        this.analyserService = analyserService;
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

    @GetMapping("/visualize")
    public void generateGraph(@RequestParam String repoName) {

    }
}
