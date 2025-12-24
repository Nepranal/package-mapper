package com.mizookie.packagemapper.services.implementations;

import com.mizookie.packagemapper.services.GithubRepositoryService;
import com.mizookie.packagemapper.services.GraphService;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.util.mxCellRenderer;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.dot.DOTImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;

@Service
public class GraphServiceImpl implements GraphService {

    // JGraphT graph to store dependencies between classes
    private Graph<String, DefaultEdge> dependencyGraph;

    @Value("${analysis.directory}")
    private String analysisDirectory;

    @Autowired
    // Constructor to initialize the graph
    public GraphServiceImpl(GithubRepositoryService githubService) {
        // Initialize a directed graph
        this.dependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    }

    public static Graph<String, DefaultEdge> createLargeGraph() {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Add vertices
        for (int i = 1; i <= 1000; i++) {
            graph.addVertex("v" + i);
        }

        // Add edges
        for (int i = 1; i < 1000; i++) {
            graph.addEdge("v" + i, "v" + (i + 1));
        }
        graph.addEdge("v1000", "v1");

        return graph;
    }

    public static Graph<String, DefaultEdge> createSmallGraph() {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Add vertices
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addVertex("D");

        // Add edges
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "D");
        graph.addEdge("D", "A");

        return graph;
    }

    public static Graph<String, DefaultEdge> createMediumGraph() {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Add vertices
        for (int i = 1; i <= 100; i++) {
            graph.addVertex("v" + i);
        }

        // Add edges
        for (int i = 1; i < 100; i++) {
            graph.addEdge("v" + i, "v" + (i + 1));
        }
        graph.addEdge("v100", "v1");

        return graph;
    }

    public Set<DefaultEdge> getOutgoingEdges(String vertex) {
        return dependencyGraph.outgoingEdgesOf(vertex);
    }

    public Set<String> getVertices() {
        return dependencyGraph.vertexSet();
    }

    // Set the dependency map (not needed with JGraphT)
    @Override
    public void setDependencyMap(Map<String, List<String>> classesMap) {
        // Clear the existing graph
        dependencyGraph.removeAllVertices(dependencyGraph.vertexSet());
        // Create a copy of the classesMap
        Map<String, List<String>> classesCopy = new HashMap<>(classesMap);

        // Add vertices and edges from the copied map
        for (Map.Entry<String, List<String>> entry : classesCopy.entrySet()) {
            String source = entry.getKey();
            dependencyGraph.addVertex(source);
            for (String target : entry.getValue()) {
                dependencyGraph.addVertex(target);
                dependencyGraph.addEdge(source, target);
            }
        }
    }

    @Override // Set the dependency map
    public void setDependencyMap(Graph<String, DefaultEdge> graph) {
        this.dependencyGraph = graph;
    }

    // Add a dependency between two classes
    @Override
    public void addEdge(String source, String target) {
        // Add vertices if they don't exist
        dependencyGraph.addVertex(source);
        dependencyGraph.addVertex(target);
        // Add the edge (dependency)
        dependencyGraph.addEdge(source, target);
    }

    // Display the graph
    @Override
    public void displayGraph(String repositoryName) {
        // Create a file to save the image
        String fileName = analysisDirectory + "/" + repositoryName + ".png";
        File imgFile = new File(fileName);

        // Create a JGraphXAdapter for the JGraphT graph
        JGraphXAdapter<String, DefaultEdge> graphAdapter = new JGraphXAdapter<>(dependencyGraph);

        // Apply a layout to the graph
        mxCircleLayout layout = new mxCircleLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());

        // Create a BufferedImage from the graph
        BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);

        // Write the BufferedImage to a file
        try {
            ImageIO.write(image, "PNG", imgFile);
            System.out.println("Graph image saved to: " + imgFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serializeGraph(String repositoryName, String version) throws IOException {
        DOTExporter<String, DefaultEdge> exporter = new DOTExporter<>();
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v));
            return map;
        });
        exporter.exportGraph(dependencyGraph, new FileWriter(String.format("%s/%s_%s.gv", analysisDirectory, repositoryName, version)));
    }

    public Graph<String, DefaultEdge> importGraph(String repositoryName) throws FileNotFoundException {
        DOTImporter<String, DefaultEdge> importer = new DOTImporter<>();
        importer.setVertexWithAttributesFactory((k, l) -> String.valueOf(l.get("label")));
        importer.importGraph(dependencyGraph, new FileReader(String.format("%s/%s.gv", analysisDirectory, repositoryName)));
        return dependencyGraph;
    }
}