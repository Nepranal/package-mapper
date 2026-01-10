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

    Set<String> getVertices();

    Set<DefaultEdge> getOutgoingEdges(String vertex);

    void serializeGraph(String s, String v) throws IOException;

    Graph<String, DefaultEdge> importGraph(String fileName) throws FileNotFoundException;
}
