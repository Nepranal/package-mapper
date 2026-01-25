package com.mizookie.packagemapper.controllers;

import com.mizookie.packagemapper.dto.user.GithubRepositoryInfoRequest;
import com.mizookie.packagemapper.services.GithubRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This class is a controller for handling requests related to GitHub repositories.
 */
@Slf4j
@RestController
@CrossOrigin()
@RequestMapping("/repository")
public class GithubRepositoryController {

    private static final String MESSAGE_KEY = "message";
    private final GithubRepositoryService githubRepositoryService;

    @Autowired
    public GithubRepositoryController(GithubRepositoryService githubRepositoryService) {
        this.githubRepositoryService = githubRepositoryService;
    }

    /**
     * This method downloads a public GitHub repository to the local file system.
     *
     * @param requestBody The request body containing the URL of the repository to download.
     * @return A response entity containing a message indicating the result of the download operation.
     */
    @PostMapping("/download")
    ResponseEntity<Map<String, Object>> downloadPublicRepository(@RequestBody GithubRepositoryInfoRequest requestBody) {
        String repositoryUrlString = requestBody.getRepositoryUrl();
        log.info("Downloading repository: {}", repositoryUrlString);
        try {
            String responseMessageString = githubRepositoryService.downloadPublicRepository(repositoryUrlString);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, responseMessageString));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    /**
     * This method deletes a GitHub repository.
     *
     * @return A response entity containing a message indicating the result of the delete operation.
     */
    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteRepository() {
        log.info("Deleting repository...");
        try {
            String responseMessageString = githubRepositoryService.deleteRepository();
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, responseMessageString));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    @GetMapping("/branches")
    List<String> getRepoBranches(@RequestParam String repo, @RequestParam(required = false) String version) throws GitAPIException, IOException {
        return githubRepositoryService.getRepoCommitVersions(repo, version, Integer.MAX_VALUE);
    }

    @PutMapping("/fetch")
    List<String> fetchRepository(@RequestParam String repo) throws GitAPIException, IOException {
        githubRepositoryService.fetchAll(repo);
        return githubRepositoryService.getLogAll(repo);
    }

    @GetMapping("/all")
    List<String> getAllRepositoryNames() {
        return githubRepositoryService.getAllRepo();
    }

    @GetMapping("/log")
    List<String> getRepositoryLogAll(@RequestParam String repo) throws GitAPIException, IOException {
        return githubRepositoryService.getLogAll(repo);
    }
}
