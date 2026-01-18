package com.mizookie.packagemapper.services;


import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public interface GraphService {
    // Set the dependency map
    void setDependencyMap(Graph<String, DefaultEdge> graph);

    // Testing Overloading
    void setDependencyMap(Map<String, List<String>> classesMap);

    // Add a dependency between two classes
    void addEdge(String source, String target);

    // Display the graph
    void displayGraph(String repositoryName);

    /**
     * Get all vertices in a graph
     */
    Set<String> getVertices();

    /**
     * Save graph as .gv file. The file will be stored with the name {@code s_v.gv}
     */
    void serializeGraph(String s, String v) throws IOException;

    /**
     * Get stored file. fileName is a .gv file but don't specify the .gv part
     *
     * @param fileName file name with no extension specified
     */
    Graph<String, DefaultEdge> importGraph(String fileName) throws FileNotFoundException;
}
