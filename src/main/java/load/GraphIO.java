package load;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class GraphIO {

    public static String mapsDir = "maps\\";
    public static String tempDir = mapsDir + "temp\\";
    private Graph graph;
    private long fileSize;
    private BiFunction<Node, Node, Double> distanceStrategy;
    private BiConsumer<Long, Long> progressListener;

    protected double progress;
    protected long bytesRead;

    public GraphIO(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
        generateFolders();
        progress = 0;
        bytesRead = 0;
    }

    public static double[] loadReach(String fileName) throws IOException {
        String fileType = Util.getFileType(fileName);
        if (fileType.equals("osm")) {
            fileName = Util.trimFileTypes(fileName);
        }
        if (fileType.equals("pbf")) {
            fileName = Util.trimFileTypes(fileName);
        }
        FileInputStream landmarksInput = new FileInputStream(tempDir + fileName + "-reach-bounds.tmp");
        ObjectInputStream landmarksStream = new ObjectInputStream(landmarksInput);

        double[] arr = null;

        try {
            arr = (double[]) landmarksStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        landmarksStream.close();

        assert arr != null;
        return arr;
    }

    private void generateFolders() {
        new File(mapsDir).mkdir();
        new File(tempDir).mkdir();
    }

    public void loadGraph(String fileName) {
        // Check for temp file to load instead
        String tmpName = tempDir + Util.trimFileTypes(fileName);
        File sccFile = new File(tmpName + "-scc-graph.tmp");
        if (sccFile.exists()) {
            loadTMP(tmpName + "-scc", "SCC graph loaded from storage");
            return;
        }
        File tmpFile = new File(tmpName + "-graph.tmp");
        if (tmpFile.exists()) {
            loadTMP(tmpName, "Graph loaded from storage");
            return;
        }
        // Load actual file
        System.out.println("No tmp files were found");
        String fileType = Util.getFileType(fileName);
        if (fileType.equals("osm")) {
            loadOSM(Util.trimFileTypes(fileName));
        }
        if (fileType.equals("pbf")) {
            loadPBF(fileName);
        }
        System.out.println("Pre-processing completed");
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

    public void storeTMP(String fileName, Graph graph) {
        try {
            String name = tempDir + fileName;
            FileOutputStream fos = new FileOutputStream(name + "-graph.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(graph);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTMP(String name, String msg) {
        try {
            String graphFileName = name + "-graph.tmp";
            fileSize = Files.size(Paths.get(graphFileName));
            CountingInputStream input = new CountingInputStream(graphFileName, this);
            ObjectInputStream objectStream = new ObjectInputStream(input);
            graph = (Graph) objectStream.readObject();
            objectStream.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println(msg);
        }
    }

    @SuppressWarnings(value = "unchecked")
    public static void loadLandmarks(String fileName, Landmarks landmarks) throws IOException {
        String fileType = Util.getFileType(fileName);
        if (fileType.equals("osm")) {
            fileName = Util.trimFileTypes(fileName);
        }
        if (fileType.equals("pbf")) {
            fileName = Util.trimFileTypes(fileName);
        }
        FileInputStream landmarksInput = new FileInputStream(tempDir + fileName + "-landmarks.tmp");
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
            next_tier += fileSize /100;
            if (bytesRead <= fileSize) {
                progressListener.accept(next_tier, fileSize);
            }
        }
    }

    public void setProgressListener(BiConsumer<Long, Long> progressListener) {
        this.progressListener = progressListener;
    }

    public Graph getGraph() {
        return graph;
    }
}


class CountingInputStream extends FileInputStream implements AutoCloseable {

    GraphIO graphIO;

    public CountingInputStream(String filename, GraphIO graphIO) throws FileNotFoundException {
        super(filename);
        this.graphIO = graphIO;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int amountBytesRead = super.read(b);
        graphIO.bytesRead += amountBytesRead;
        graphIO.updateProgress();
        return amountBytesRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int amountBytesRead = super.read(b, off, len);
        graphIO.bytesRead += amountBytesRead;
        graphIO.updateProgress();
        return amountBytesRead;
    }



    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            graphIO.bytesRead++;
            graphIO.updateProgress();
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}