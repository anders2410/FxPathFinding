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
import paths.Landmarks;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class GraphImport {

    public static String mapsDir = "maps\\";
    public static String tempDir = mapsDir + "temp\\";
    private long tempCombinedSize;
    private Graph graph;
    private BiFunction<Node, Node, Double> distanceStrategy;
    private BiConsumer<Long, Long> progressListener;

    protected double progress;
    protected long bytesRead;

    public GraphImport(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
        generateFolders();
        progress = 0;
        bytesRead = 0;
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
                System.out.println("Graph loaded from harddrive\n");
            } else {
                loadPBF(fileName);
                System.out.println("No files were found. Pre-processing has completed");
            }
        }
        if (fileType.equals("scc")) {
            loadSCC();
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
            PBFParser pbfParser = new PBFParser(fileName);
            pbfParser.setStoreTMPListener(this::storeTMP);
            pbfParser.setDistanceStrategy(distanceStrategy);
            pbfParser.executePBFParser();
            graph = pbfParser.getGraph();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storeTMP(String fileName, Graph graph) {
        try {
            String name = tempDir + fileName;
            FileOutputStream fos = new FileOutputStream(name + "-node-list.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(graph.getNodeList());
            oos.close();

            FileOutputStream fos1 = new FileOutputStream(name + "-adj-list.tmp");
            ObjectOutputStream oos1 = null;
            oos1 = new ObjectOutputStream(fos1);
            oos1.writeObject(graph.getAdjList());
            oos1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void loadTMP(String name) throws IOException {
        String nodeListFileName = name + "-node-list.tmp";
        String adjListFileName = name + "-adj-list.tmp";
        long nodeListSize = Files.size(Paths.get(nodeListFileName));
        long adjListSize = Files.size(Paths.get(adjListFileName));
        tempCombinedSize = nodeListSize + adjListSize;
        CountingInputStream nodeCountInput = new CountingInputStream(nodeListFileName, this);
        CountingInputStream adjCountInput = new CountingInputStream(adjListFileName, this);

        ObjectInputStream nodeStream = new ObjectInputStream(nodeCountInput);
        ObjectInputStream edgeStream = new ObjectInputStream(adjCountInput);
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

    private void loadSCC() {

    }

    @SuppressWarnings(value = "unchecked")
    public static void loadLandmarks(String name, Landmarks landmarks) throws IOException {
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
        landmarks.setLandmarkSet(landmarksSet);
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

    long next_tier = 0;

    public void updateProgress() {
        if (next_tier <= bytesRead) {
            next_tier += tempCombinedSize/100;
            if (bytesRead <= tempCombinedSize) {
                progressListener.accept(next_tier, tempCombinedSize);
            }
        }
    }

    public void setProgressListener(BiConsumer<Long, Long> progressListener) {
        this.progressListener = progressListener;
    }
}


class CountingInputStream extends FileInputStream implements AutoCloseable {

    GraphImport graphImport;

    public CountingInputStream(String filename, GraphImport graphImport) throws FileNotFoundException {
        super(filename);
        this.graphImport = graphImport;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int amountBytesRead = super.read(b);
        graphImport.bytesRead += amountBytesRead;
        graphImport.updateProgress();
        return amountBytesRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int amountBytesRead = super.read(b, off, len);
        graphImport.bytesRead += amountBytesRead;
        graphImport.updateProgress();
        return amountBytesRead;
    }



    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            graphImport.bytesRead++;
            graphImport.updateProgress();
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}