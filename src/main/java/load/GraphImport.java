package load;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Edge;
import model.Graph;
import model.Node;
import load.pbfparsing.PBFParser;
import load.xml.XMLFilter;
import load.xml.XMLGraphExtractor;
import model.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class GraphImport {

    public static String mapsDir = "maps\\";
    public static String tempDir = mapsDir + "temp\\";

    private Graph graph;
    private BiFunction<Node, Node, Double> distanceStrategy;

    public GraphImport(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
        generateFolders();
    }

    private void generateFolders() {
        new File(mapsDir).mkdir();
        new File(tempDir).mkdir();
    }

    public Graph loadGraph(String fileName) {
        String fileType = fileName.substring(fileName.length() - 3);
        if (fileType.equals("osm")) {
            loadOSM(fileName.substring(0, fileName.length() - 4));
        }
        if (fileType.equals("pbf")) {
            String name = tempDir + fileName.substring(0, fileName.indexOf('.'));
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

    public Graph loadOSMOld(String fileName) {
        XMLFilter xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.setParseCordStrategy(Util::cordToInt);
        xmlGraphExtractor.setDistanceStrategy(distanceStrategy);
        xmlGraphExtractor.executeExtractor();
        return xmlGraphExtractor.getGraph();
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

    @SuppressWarnings(value = "unchecked")
    public static void loadLandmarks(String name, Graph graph) throws IOException {
        String fileType = name.substring(name.length() - 3);
        if (fileType.equals("osm")) {
            name = (name.substring(0, name.length() - 4));
        }
        if (fileType.equals("pbf")) {
            name = name.substring(0, name.indexOf('.'));
        }
        FileInputStream landmarksInput = new FileInputStream(tempDir + name + "-landmarks.tmp");
        ObjectInputStream landmarksStream = new ObjectInputStream(landmarksInput);

        Set<Integer> landmarksSet = null;

        try {
            landmarksSet = (Set<Integer>) landmarksStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        landmarksStream.close();

        assert landmarksSet != null;
        graph.setLandmarks(landmarksSet);
    }

    public static File selectMapFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir") + "\\" + mapsDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PBF Files", "*.pbf"),
                new FileChooser.ExtensionFilter("OSM Files", "*.osm")
        );
        return fileChooser.showOpenDialog(stage);
    }
}
