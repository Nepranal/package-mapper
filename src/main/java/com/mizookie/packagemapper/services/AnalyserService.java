package com.mizookie.packagemapper.services;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AnalyserService {
    void visualize(Map<String, List<String>> classesMap); // visualize the parsed data

    void analyse(String repositoryPath, String version) throws IOException, GitAPIException, InterruptedException; // orchestrate the crawling, parsing and visualization

    void analyse() throws IOException, GitAPIException, InterruptedException; // analyze all repositories

    void visualizeDemo(); // visualize the parsed data
}
