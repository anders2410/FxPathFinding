package load;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Graph;
import info_model.GraphInfo;
import model.Node;
import load.pbfparsing.PBFParser;
import load.xml.XMLFilter;
import load.xml.XMLGraphExtractor;
import paths.LandmarkMode;
import paths.Util;
import paths.Landmarks;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static paths.LandmarkMode.*;

//TODO: Autogenerate folders
public class GraphIO {

    public static String mapsDir = "maps\\";
    private Graph graph;
    private GraphInfo graphInfo;
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

    public static String getFolderName(String fileName) {
        return mapsDir + Util.trimFileTypes(fileName) + "\\";
    }

    public void saveLandmarks(String fileName, LandmarkMode generationMode, Set<Integer> landmarkSet) {
        try {
            String name = getFolderName(fileName) + Util.trimFileTypes(fileName);
            FileOutputStream fos = new FileOutputStream(name + "-" + generationMode.toString() + "landmarks.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(landmarkSet);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateFolders() {
        new File(mapsDir).mkdir();
        File[] files = new File(mapsDir).listFiles(file -> !file.isDirectory());
        if (files == null) {
            return;
        }
        for (File file : files) {
            new File(Util.trimFileTypes(file.getAbsolutePath())).mkdir();
        }
    }

    public LoadType loadGraph(String fileName) {
        // Check for temp file to load instead
        String tmpName = getFolderName(fileName) + Util.trimFileTypes(fileName);
        File sccFile = new File(tmpName + "-scc-graph.tmp");
        if (sccFile.exists()) {
            loadTMP(tmpName + "-scc", "SCC graph loaded from storage");
            return LoadType.SCC;
        }
        File tmpFile = new File(tmpName + "-graph.tmp");
        if (tmpFile.exists()) {
            loadTMP(tmpName, "Graph loaded from storage");
            return LoadType.TEMP;
        }
        // Load actual file
        System.out.println("No tmp files were found");
        String fileType = Util.getFileType(fileName);
        if (fileType.equals("osm")) {
            loadOSM(Util.trimFileTypes(fileName));
            System.out.println("Pre-processing completed");
            return LoadType.OSM;
        }
        if (fileType.equals("pbf")) {
            loadPBF(fileName);
            System.out.println("Pre-processing completed");
            return LoadType.PBF;
        }
        System.out.println("Error in loading");
        return LoadType.NONE;
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
            graphInfo = pbfParser.getGraphInfo();
            storeGraphInfo(Util.trimFileTypes(fileName), graphInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeTMP(String fileName, Graph graph) {
        try {
            String name = getFolderName(fileName) + fileName;
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

    public void storeGraphInfo(String fileName, GraphInfo graphInfo) {
        try {
            String name = getFolderName(fileName) + fileName;
            FileOutputStream fos = new FileOutputStream(name + "-info.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(graphInfo);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadGraphInfo(String fileName, String msg) {
        String infoName = getFolderName(fileName) + fileName;
        File infoFile = new File(infoName + "-info.tmp");
        if (!infoFile.exists()) {
            return;
        }
        try {
            String graphFileName = getFolderName(fileName) + fileName + "-info.tmp";
            fileSize = Files.size(Paths.get(graphFileName));
            CountingInputStream input = new CountingInputStream(graphFileName, this);
            ObjectInputStream objectStream = new ObjectInputStream(input);
            graphInfo = (GraphInfo) objectStream.readObject();
            objectStream.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println(msg);
        }
    }

    public void loadBestLandmarks(String fileName, Landmarks landmarks) {
        if (tryMode(fileName, landmarks, MAXCOVER)) return;
        if (tryMode(fileName, landmarks, AVOID)) return;
        if (tryMode(fileName, landmarks, FARTHEST)) return;
        tryMode(fileName, landmarks, RANDOM);
    }

    private boolean tryMode(String fileName, Landmarks landmarks, LandmarkMode mode) {
        String maxCoverFileName = getLandmarksFilePathName(fileName, mode);
        if (new File(maxCoverFileName).exists()) {
            loadLandmarks(fileName, mode, landmarks);
            return true;
        }
        return false;
    }

    @SuppressWarnings(value = "unchecked")
    public void loadLandmarks(String fileName, LandmarkMode mode, Landmarks landmarks) {
        try {
            String name = getLandmarksFilePathName(fileName, mode);
            FileInputStream landmarksInput = new FileInputStream(name);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLandmarksFilePathName(String fileName, LandmarkMode mode) {
        return getFolderName(fileName) + Util.trimFileTypes(fileName) + "-" + mode.toString() + "landmarks.tmp";
    }

    public List<Double> loadReach(String fileName) {
        String fileType = Util.getFileType(fileName);
        if (fileType.equals("osm")) {
            fileName = Util.trimFileTypes(fileName);
        }
        if (fileType.equals("pbf")) {
            fileName = Util.trimFileTypes(fileName);
        }
        try {
            String reachFile = getFolderName(fileName) + fileName + "-reach-bounds.tmp";
            if (!new File(reachFile).exists()) {
                return null;
            }
            FileInputStream reachInput = new FileInputStream(reachFile);
            ObjectInputStream reachStream = new ObjectInputStream(reachInput);

            List<Double> bounds = null;

            try {
                bounds = (List<Double>) reachStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            reachStream.close();
            assert bounds != null;
            return bounds;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveReach(String fileName, List<Double> reachBounds) {
        try {
            String name = getFolderName(fileName) + Util.trimFileTypes(fileName);
            FileOutputStream fos = new FileOutputStream(name + "-reach-bounds.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(reachBounds);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        if (progressListener == null) return;
        if (next_tier <= bytesRead) {
            next_tier += fileSize / 100;
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

    public GraphInfo getGraphInfo() {
        return graphInfo;
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