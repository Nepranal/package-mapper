package com.mizookie.packagemapper.services;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * This interface represents a service for interacting with GitHub repositories.
 */
@Service
public interface GithubRepositoryService {
    String downloadPublicRepository(String repositoryUrlString) throws GitAPIException;

    String deleteRepository() throws IOException, GitAPIException;

    void downloadPrivateRepository(String repositoryUrlString, String token);

    /**
     * Get the current commit version of {@code repositoryName}
     */
    String getCurrentCommit(String repositoryName) throws GitAPIException, IOException;

    /**
     * Get commit version of {@code repositoryName} based on {@code version} which could be limited by {@code limit}.
     * If you don't want to limit, then just set limit to be the maximum integer number
     */
    List<String> getRepoCommitVersions(String repositoryName, String version, int limit) throws GitAPIException, IOException;

    void checkoutCommit(String repositoryName, String version) throws IOException, GitAPIException;

    /**
     * Perform {@code git fetch} for all remote branches
     */
    void fetchAll(String repositoryName) throws IOException, GitAPIException;

    /**
     * get all cloned repository names
     */
    List<String> getAllRepo();

    /**
     * Get all reachable repository commit versions
     */
    List<String> getLogAll(String repositoryName) throws GitAPIException, IOException;
}
