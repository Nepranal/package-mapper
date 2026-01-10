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

    String getCurrentCommit(String repositoryName) throws GitAPIException, IOException;

    List<String> getRepoCommitVersions(String repositoryName, String version, int limit) throws GitAPIException, IOException;

    void checkoutCommit(String repositoryName, String version) throws IOException, GitAPIException;

    void fetchAll(String repositoryName) throws IOException, GitAPIException;

    public List<String> getAllRepo();

    List<String> getLogAll(String repositoryName) throws GitAPIException, IOException;
}
