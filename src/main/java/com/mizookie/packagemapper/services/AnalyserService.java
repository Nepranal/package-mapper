package com.mizookie.packagemapper.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AnalyserService {
    void visualize(Map<String, List<String>> classesMap); // visualize the parsed data

    void analyse(String repositoryPath) throws IOException; // orchestrate the crawling, parsing and visualization

    void analyse() throws IOException; // analyze all repositories

    void visualizeDemo(); // visualize the parsed data
}
