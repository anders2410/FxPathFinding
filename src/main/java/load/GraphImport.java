package load;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Edge;
import model.Graph;
import model.Node;
import pbfparsing.PBFParser;
import xml.XMLFilter;
import xml.XMLGraphExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.function.BiFunction;

public class GraphImport {

    private Graph graph;
    private BiFunction<Node, Node, Double> distanceStrategy;

    public GraphImport(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
    }

    public Graph loadGraph(String fileName) {
        String fileType = fileName.substring(fileName.length() - 3);
        if (fileType.equals("osm")) {
            loadOSM(fileName.substring(0, fileName.length() - 4));
        }
        if (fileType.equals("pbf")) {
            String name = fileName.substring(0, fileName.indexOf('.'));
            File nodeFile = new File(name + "-node-list.tmp");
            File adjFile = new File(name + "-adj-list.tmp");
            if (nodeFile.exists() && adjFile.exists()) {
                try {
                    loadTMP(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("No pre-processing has been made\n");
            } else {
                loadPBF(fileName);
                System.out.println("No files were found. Pre-processing has completed");
            }
        }
        return graph;
    }

    private void loadOSM(String fileName) {
        XMLFilter xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.setDistanceStrategy(distanceStrategy);
        xmlGraphExtractor.executeExtractor();
        graph = xmlGraphExtractor.getGraph();
    }

    private void loadPBF(String fileName) {
        try {
            PBFParser pbfParser = new PBFParser(fileName, true);
            pbfParser.setDistanceStrategy(distanceStrategy);
            pbfParser.executePBFParser();
            graph = pbfParser.getGraph();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void loadTMP(String name) throws IOException {
        FileInputStream nodeInput = new FileInputStream(name + "-node-list.tmp");
        FileInputStream edgeInput = new FileInputStream(name + "-adj-list.tmp");

        ObjectInputStream nodeStream = new ObjectInputStream(nodeInput);
        ObjectInputStream edgeStream = new ObjectInputStream(edgeInput);

        List<Node> nodeList = null;
        List<List<Edge>> adjList = null;

        try {
            nodeList = (List<Node>) nodeStream.readObject();
            adjList = (List<List<Edge>>) edgeStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        nodeStream.close();
        edgeStream.close();

        assert nodeList != null;
        graph = new Graph(nodeList.size());

        graph.setNodeList(nodeList);
        graph.setAdjList(adjList);
    }

    public static File selectMapFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PBF Files", "*.pbf"),
                new FileChooser.ExtensionFilter("OSM Files", "*.osm")
        );
        return fileChooser.showOpenDialog(stage);
    }
}
