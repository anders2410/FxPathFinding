package load;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Graph;
import info_model.GraphInfo;
import model.Node;
import load.pbfparsing.PBFParser;
import load.xml.XMLFilter;
import load.xml.XMLGraphExtractor;
import paths.AlgorithmMode;
import paths.SSSP;
import paths.preprocessing.CHResult;
import paths.preprocessing.LandmarkMode;
import paths.Util;
import paths.preprocessing.Landmarks;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static paths.preprocessing.LandmarkMode.*;

public class GraphIO {

    public static String mapsDir = "maps\\";
    private Graph graph;
    private GraphInfo graphInfo;
    private long fileSize;
    private BiFunction<Node, Node, Double> distanceStrategy;
    private BiConsumer<Long, Long> progressListener;
    private boolean isSCCGraph;

    protected double progress;
    protected long bytesRead;

    public GraphIO(BiFunction<Node, Node, Double> distanceStrategy, boolean isSCCGraph) {
        this.distanceStrategy = distanceStrategy;
        this.isSCCGraph = isSCCGraph;
        generateFolders();
        progress = 0;
        bytesRead = 0;
    }

    public GraphIO(boolean isSCCGraph) {
        this.distanceStrategy = Util::sphericalDistance;
        this.isSCCGraph = isSCCGraph;
        generateFolders();
        progress = 0;
        bytesRead = 0;
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

    public void loadPreAll(String fileName) {
        loadPreREAL(fileName);
        SSSP.setCHResult(loadCH(fileName));
    }

    public void loadPreCH(String fileName) {
        loadGraph(fileName);
        SSSP.setGraph(graph);
        SSSP.setCHResult(loadCH(fileName));
    }

    public void loadPreREAL(String fileName) {
        loadPreALT(fileName);
        SSSP.setReachBounds(loadReach(fileName));
    }

    public void loadPreALT(String fileName) {
        loadGraph(fileName);
        SSSP.setGraph(graph);

        SSSP.setDensityMeasures(loadDensities(fileName));

        Landmarks landmarks = new Landmarks(graph);
        loadBestLandmarks(fileName, landmarks);
        SSSP.setLandmarks(landmarks);
        SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
    }

    public LoadType loadGraph(String fileName) {
        // Check for temp file to load instead
        String folderName = getFolderName(fileName) + Util.trimFileTypes(fileName);
        File sccFile = new File(folderName + "-scc-graph.tmp");
        if (sccFile.exists()) {
            loadGraph(folderName + "-scc", "SCC graph loaded from storage");
            return LoadType.SCC;
        }
        File tmpFile = new File(folderName + "-graph.tmp");
        if (tmpFile.exists()) {
            loadGraph(folderName, "Graph loaded from storage");
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
            loadPBF(fileName, false);
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

    private void loadPBF(String fileName, boolean parseInfo) {
        try {
            PBFParser pbfParser = new PBFParser(fileName, parseInfo);
            pbfParser.setStoreTMPListener(this::storeGraph);
            pbfParser.setDistanceStrategy(distanceStrategy);
            pbfParser.setProgressListener(progressListener);
            pbfParser.executePBFParser();
            graph = pbfParser.getGraph();
            if (parseInfo) {
                graphInfo = pbfParser.getGraphInfo();
                storeGraphInfo(Util.trimFileTypes(fileName), graphInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeGraph(String fileName, Graph graph) {
        try {
            String name = getTrimmedFolderSCCName(fileName) + "-graph.tmp";
            FileOutputStream fos = new FileOutputStream(name);
            OutputStream buffer = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(graph);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGraph(String name, String msg) {
        try {
            String graphFileName = name + "-graph.tmp";
            fileSize = Files.size(Paths.get(graphFileName));
            CountingInputStream input = new CountingInputStream(graphFileName, this);
            InputStream buffer = new BufferedInputStream(input);
            ObjectInputStream objectStream = new ObjectInputStream(buffer);
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
            String name = getTrimmedFolderSCCName(fileName) + "-info.tmp";
            FileOutputStream fos = new FileOutputStream(name);
            OutputStream buffer = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(graphInfo);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LoadType parseGraphInfo(String filename) {
        if (Util.getFileType(filename).equals("osm")) {
            System.out.println("Can't load info from .osm file");
            return LoadType.NONE;
        }

        String infoNameSCC = getFolderName(filename) + Util.trimFileTypes(filename) + "-scc-info.tmp";
        File infoFileSCC = new File(infoNameSCC);
        if (isSCCGraph && !infoFileSCC.exists()) {
            isSCCGraph = false;
        }

        String infoName = getTrimmedFolderSCCName(filename) + "-info.tmp";
        File infoFile = new File(infoName);

        if (infoFile.exists()) {
            loadGraphInfo(filename);
            return isSCCGraph ? LoadType.SCC : LoadType.TEMP;
        } else {
            loadPBF(filename, true);
            System.out.println("Info parsed from pbf file");
            return LoadType.PBF;
        }
    }

    public void loadGraphInfo(String fileName) {
        String infoName = getTrimmedFolderSCCName(fileName) + "-info.tmp";
        File infoFile = new File(infoName);
        if (!infoFile.exists()) {
            return;
        }
        try {
            fileSize = Files.size(Paths.get(infoName));
            CountingInputStream input = new CountingInputStream(infoName, this);
            InputStream buffer = new BufferedInputStream(input);
            ObjectInputStream objectStream = new ObjectInputStream(buffer);
            graphInfo = (GraphInfo) objectStream.readObject();
            objectStream.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Loaded info from tmp file");
        }
    }

    public void saveLandmarks(String fileName, LandmarkMode generationMode, Set<Integer> landmarkSet) {
        try {
            String name = getTrimmedFolderSCCName(fileName) + "-" + generationMode.toString() + "landmarks.tmp";
            FileOutputStream fos = new FileOutputStream(name);
            OutputStream buffer = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(landmarkSet);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            InputStream buffer = new BufferedInputStream(landmarksInput);
            ObjectInputStream landmarksStream = new ObjectInputStream(buffer);

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
        return getTrimmedFolderSCCName(fileName) + "-" + mode.toString() + "landmarks.tmp";
    }

    @SuppressWarnings("unchecked")
    public List<Double> loadReach(String fileName) {
        try {
            String reachFile = getTrimmedFolderSCCName(fileName) + "-reach-bounds.tmp";
            if (!new File(reachFile).exists()) {
                return null;
            }
            FileInputStream reachInput = new FileInputStream(reachFile);
            InputStream buffer = new BufferedInputStream(reachInput);
            ObjectInputStream reachStream = new ObjectInputStream(buffer);

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
            String name = getTrimmedFolderSCCName(fileName) + "-reach-bounds.tmp";
            FileOutputStream fos = new FileOutputStream(name);
            OutputStream buffer = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(reachBounds);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CHResult loadCH(String fileName) {
        try {
            String chFile = getTrimmedFolderSCCName(fileName) + "-contraction-hierarchies.tmp";
            if (!new File(chFile).exists()) {
                return null;
            }
            FileInputStream chInput = new FileInputStream(chFile);
            InputStream buffer = new BufferedInputStream(chInput);
            ObjectInputStream chStream = new ObjectInputStream(buffer);

            CHResult contractionHierarchies = null;

            try {
                contractionHierarchies = (CHResult) chStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            chStream.close();
            assert contractionHierarchies != null;
            System.out.println("Loaded Contraction Hierarchies successfully!");
            return contractionHierarchies;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveCH(String fileName, CHResult CHResult) {
        try {
            String name = getTrimmedFolderSCCName(fileName) + "-contraction-hierarchies.tmp";
            FileOutputStream fos = new FileOutputStream(name);
            OutputStream buffer = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(CHResult);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> loadDensities(String fileName) {
        try {
            String densityFile = getTrimmedFolderSCCName(fileName) + "-densities.tmp";
            if (!new File(densityFile).exists()) {
                return null;
            }
            FileInputStream densityInput = new FileInputStream(densityFile);
            InputStream buffer = new BufferedInputStream(densityInput);
            ObjectInputStream reachStream = new ObjectInputStream(buffer);

            List<Integer> densityMeasures = null;

            try {
                densityMeasures = (List<Integer>) reachStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            reachStream.close();
            assert densityMeasures != null;
            return densityMeasures;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void storeDensities(String fileName, List<Integer> densityMeasures) {
        try {
            String sccName = getTrimmedFolderSCCName(fileName) + "-densities.tmp";
            FileOutputStream fos = new FileOutputStream(sccName);
            OutputStream buffer = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(densityMeasures);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File selectMapFile(Stage stage) {
        if(true)
        return selectMapDirectory(stage);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir") + "\\" + mapsDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PBF Files", "*.pbf"),
                new FileChooser.ExtensionFilter("OSM Files", "*.osm")
        );
        return fileChooser.showOpenDialog(stage);
    }

    public static File selectMapDirectory(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.dir") + "\\" + mapsDir));
        File dir = directoryChooser.showDialog(stage);
        if (dir == null) {
            return null;
        }
        String dirName = dir.getAbsolutePath();
        File pbfFile = new File(dirName + ".osm.pbf");
        File osmFile = new File(dirName + ".osm");
        if (osmFile.exists()) {
            return osmFile;
        } else {
            return pbfFile;
        }
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

    public static String getFolderName(String fileName) {
        return mapsDir + Util.trimFileTypes(fileName) + "\\";
    }

    private String applySCCSuffix() {
        return isSCCGraph ? "-scc" : "";
    }

    private String getTrimmedFolderSCCName(String fileName) {
        return getFolderName(fileName) + Util.trimFileTypes(fileName) + applySCCSuffix();
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

    public boolean fileExtensionExists(String filename, String extension) {
        String folderName = getTrimmedFolderSCCName(filename);
        File file = new File(folderName + extension);
        return file.exists();
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