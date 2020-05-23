package javafx;

import info_model.EdgeInfo;
import info_model.GraphInfo;
import info_model.GraphPair;
import info_model.NodeInfo;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import load.GraphIO;
import load.LoadType;
import model.Edge;
import model.Graph;
import model.ModelUtil;
import model.Node;
import paths.*;
import paths.generator.EdgeWeightGenerator;
import paths.preprocessing.*;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import static paths.AlgorithmMode.*;
import static paths.SSSP.seed;
import static paths.Util.algorithmNames;

/**
 * The controller class for JavaFX. It handles all functions related to interacting with the GUI. It contain
 * functions for all buttons, labels and other stuff that need to be updated during runtime.
 */
public class FXMLController implements Initializable {

    // Variables passed from the scene.fxml (instantiated by JavaFX itself)
    @FXML
    private Canvas canvas;
    @FXML
    private Label algorithm_label;
    @FXML
    private Label distance_label;
    @FXML
    private Label nodes_visited_label;
    @FXML
    private Label nodes_label;
    @FXML
    private Label edges_label;
    @FXML
    private Label source_label;
    @FXML
    private Label target_label;
    @FXML
    private Label seed_label;
    @FXML
    private ProgressIndicator progress_indicator;

    private Stage stage;

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    private Graph graph;
    private GraphInfo graphInfo;
    private Landmarks landmarksGenerator;
    private GraphicsContext gc;
    private boolean isSCCGraph = false;

    private BiFunction<Node, Node, Double> distanceStrategy;
    private AlgorithmMode algorithmMode = DIJKSTRA;
    private Deque<Node> selectedNodes = new ArrayDeque<>();
    private int mouseNodes = 0;
    private String fileName;
    private LandmarkMode landmarksGenMode;
    private List<Integer> densityMeasures;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        distanceStrategy = Util::sphericalDistance;
        canvas.setOnMouseClicked(onMouseClicked());
        canvas.setOnMousePressed(onMousePressed());
        canvas.setOnMouseDragged(onMouseDragged());
        canvas.setOnScroll(onMouseScrolled());
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);
        loadNewGraph("malta-latest.osm.pbf");
        setWindowChangeListener();
        SSSP.setDistanceStrategy(distanceStrategy);
        setSeedLabel();
    }

    /**
     * Starts a loadGraph thread.
     *
     * @param fileName file to load.
     */
    private void loadNewGraph(String fileName) {
        isSCCGraph = false;
        onRightClick();
        if (fileName == null || fileName.equals("")) {
            return;
        }
        this.fileName = fileName;
        Task<Graph> loadGraphTask = new Task<>() {
            @Override
            protected Graph call() {
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                graphIO.setProgressListener(this::updateProgress);
                LoadType lt = graphIO.loadGraph(fileName);
                isSCCGraph = lt == LoadType.SCC;
                return graphIO.getGraph();
            }
        };
        loadGraphTask.setOnSucceeded(event -> {
            graph = loadGraphTask.getValue();
            GraphIO graphIOInfo = new GraphIO(distanceStrategy, isSCCGraph);
            graphIOInfo.loadGraphInfo(fileName);
            graphInfo = graphIOInfo.getGraphInfo();
            landmarksGenerator = new Landmarks(graph);
            new GraphIO(distanceStrategy, isSCCGraph).loadBestLandmarks(fileName, landmarksGenerator);
            SSSP.setReachBounds(null);
            loadReachBounds();
            loadContractionHierarchies();
            loadDensities();
            setUpGraph();
            playIndicatorCompleted();
        });
        loadGraphTask.setOnFailed(event -> {
            playIndicatorCompleted();
            displayFailedDialog("load graph " + Util.trimFileTypes(fileName), event);
        });
        attachProgressIndicator(loadGraphTask.progressProperty());
        new Thread(loadGraphTask).start();
    }

    private void storeGraph() {
        Task<Void> storeGraphTask = new Task<>() {
            @Override
            protected Void call() {
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                String name = Util.trimFileTypes(fileName);
                graphIO.storeGraph(name, graph);
                if (graphInfo != null) {
                    graphIO.storeGraphInfo(name, graphInfo);
                }
                return null;
            }
        };
        storeGraphTask.setOnFailed(e -> displayFailedDialog("store graph", e));
        new Thread(storeGraphTask).start();
    }

    /**
     * Is called on the GUI thread when a loadGraph thread is finished.
     */
    public void setUpGraph() {
        mouseNodes = 0;
        if (gc != null) {
            nodes_label.setText("Nodes in Graph: " + graph.getNodeAmount());
            edges_label.setText("Edges in Graph: " + graph.getEdgeAmount());
        }
        fitGraph();
        SSSP.setGraph(graph);
        SSSP.setGraphInfo(graphInfo);
        SSSP.setLandmarks(landmarksGenerator);
        resetSelection();
    }

    private void parseInfo() {
        Task<LoadType> graphInfoTask = new Task<>() {
            @Override
            protected LoadType call() {
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                graphIO.setProgressListener(this::updateProgress);
                LoadType loadType = graphIO.parseGraphInfo(fileName);
                graphInfo = graphIO.getGraphInfo();
                updateProgress(1,1);
                return loadType;
            }
        };
        graphInfoTask.setOnSucceeded(e -> {
            LoadType loadType = graphInfoTask.getValue();
            playIndicatorCompleted();
            if (isSCCGraph && (loadType == LoadType.TEMP || loadType == LoadType.PBF)) {
                graphInfo = new ModelUtil(graph).subGraph(graphInfo, graph.getSccNodeSet());
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                graphIO.storeGraphInfo(fileName, graphInfo);
            }
            SSSP.setGraphInfo(graphInfo);
        });
        graphInfoTask.setOnFailed(event -> {
            playIndicatorCompleted();
            displayFailedDialog("load graph info " + Util.trimFileTypes(fileName), event);
        });
        attachProgressIndicator(graphInfoTask.progressProperty());
        new Thread(graphInfoTask).start();
    }

    private Task<List<ShortestPathResult>> ssspTask;

    private void runAlgorithm() {
        if (selectedNodes.size() <= 1) {
            return;
        }
        currentResult = new ShortestPathResult();
        ssspTask = new Task<>() {
            @Override
            protected List<ShortestPathResult> call() {
                return ssspConnectingNodes(new ArrayDeque<>(selectedNodes));
            }
        };
        ssspTask.setOnSucceeded(event -> {
            currentResult = combineResults(ssspTask.getValue());
            setLabels(Util.roundDouble(currentResult.d), currentResult.scannedNodesA.size() + currentResult.scannedNodesB.size());
            redrawGraph();
        });
        ssspTask.setOnFailed(e -> displayFailedDialog("run algorithm", e));
        new Thread(ssspTask).start();
    }

    private ShortestPathResult combineResults(List<ShortestPathResult> results) {
        //if (results == null) return new ShortestPathResult();
        return results.stream().reduce((res1, res2) -> {
            List<Integer> combinedPath = new ArrayList<>(res1.path);
            combinedPath.addAll(res2.path.subList(1, res2.path.size()));
            Set<Integer> combinedScannedA = new HashSet<>(res1.scannedNodesA);
            combinedScannedA.addAll(res2.scannedNodesA);
            Set<Integer> combinedScannedB = new HashSet<>(res1.scannedNodesB);
            combinedScannedA.addAll(res2.scannedNodesB);
            Set<Edge> combinedRelaxedA = new HashSet<>(res1.relaxedEdgesA);
            combinedRelaxedA.addAll(res2.relaxedEdgesA);
            Set<Edge> combinedRelaxedB = new HashSet<>(res1.relaxedEdgesB);
            combinedRelaxedA.addAll(res2.relaxedEdgesB);
            return new ShortestPathResult(res1.d + res2.d, combinedPath, combinedScannedA, combinedScannedB, combinedRelaxedA, combinedRelaxedB, res1.runTime + res2.runTime);
        }).orElseGet(ShortestPathResult::new);
    }

    private List<ShortestPathResult> ssspConnectingNodes(Deque<Node> nodeQueue) {
        List<ShortestPathResult> results = new ArrayList<>();
        Node firstNode = nodeQueue.pollLast();
        nodeQueue.addFirst(firstNode);
        while (firstNode != nodeQueue.peekLast()) {
            Node fromNode = nodeQueue.pollLast();
            Node toNode = nodeQueue.peekFirst();
            nodeQueue.addFirst(fromNode);
            assert fromNode != null && toNode != null;
            results.add(SSSP.findShortestPath(fromNode.index, toNode.index, algorithmMode));
        }
        return results;
    }

    private ShortestPathResult currentResult = new ShortestPathResult();

    private void cancelAlgorithm() {
        if (ssspTask != null) {
            ssspTask.cancel();
        }
    }

    /**
     * The main method that draws the Graph inside the Canvas. It iterates through all
     * the nodes and draw the Edges.
     */
    private void redrawGraph() {
        clearCanvas();
        drawAllEdges();
        drawSelectedNodes();
        drawOverlayNodes();
        drawAllLandmarks();
    }

    private void drawSelectedNodes() {
        if (selectedNodes.isEmpty()) {
            return;
        }
        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLACK);
        Node from = selectedNodes.removeFirst();
        drawNode(from);
        gc.setFill(Color.DARKRED);
        for (Node node : selectedNodes) {
            drawNode(node);
        }
        selectedNodes.addFirst(from);
    }

    List<Node> overlayNodes = new ArrayList<>();
    List<Node> overlayNodes2 = new ArrayList<>();

    private void resetOverlays() {
        overlayNodes = new ArrayList<>();
        overlayNodes2 = new ArrayList<>();
    }

    private void drawOverlayNodes() {
        if (currentOverlay == OverlayType.NODEPAIRS) {
            gc.setStroke(Color.BLACK);
            for (int i = 0; i < overlayNodes.size(); i++) {
                gc.setFill(Color.GREEN);
                drawNode(overlayNodes.get(i));
                gc.setFill(Color.DARKRED);
                if (i < overlayNodes2.size()) {
                    drawNode(overlayNodes2.get(i));
                }
            }
        }
    }

    private void drawNode(Node node) {
        PixelPoint np = toScreenPos(node);
        double radius = 6;
        double shift = radius / 2;
        gc.fillOval(np.x - shift, np.y - shift, radius, radius);
        gc.strokeOval(np.x - shift, np.y - shift, radius, radius);
    }

    Set<Edge> mouseEdges = new HashSet<>();
    Set<Edge> drawnEdges;

    private void drawAllEdges() {
        List<Node> nodeList = graph.getNodeList();
        List<List<Edge>> adjList = graph.getAdjList();
        drawnEdges = new HashSet<>();
        // Iterates through all nodes
        for (int i = 0; i < adjList.size(); i++) {
            Node from = nodeList.get(i);
            // Iterates through adjacent nodes/edges
            for (Edge edge : adjList.get(i)) {
                Node to = nodeList.get(edge.to);
                Edge oppositeEdge = findOppositeEdge(adjList, i, edge);
                // If the edge doesn't violate these constrains it will be drawn in the Canvas.
                if (oppositeEdge == null || isBetter(from, edge, to, oppositeEdge)) {
                    PixelPoint pFrom = toScreenPos(from);
                    PixelPoint pTo = toScreenPos(to);
                    gc.setStroke(makeColor(edge));
                    gc.setLineWidth(chooseEdgeWidth(edge));
                    if (mouseEdges.contains(edge)) {
                        gc.setLineDashes(7);
                    } else {
                        gc.setLineDashes(0);
                    }
                    gc.strokeLine(pFrom.x, pFrom.y, pTo.x, pTo.y);
                    drawnEdges.add(edge);
                }
            }
        }
    }

    private OverlayType currentOverlay = OverlayType.NONE;

    private Color makeColor(Edge edge) {
        Color color = chooseAlgorithmEdgeColor(edge);
        if (currentOverlay == OverlayType.REACH && SSSP.getReachBounds() != null) {
            findMaxReach();
            color = graduateColorSaturation(color, SSSP.getReachBounds().get(edge.from), maxReach);
        }
        if (currentOverlay == OverlayType.CH && SSSP.getCHResult() != null) {
            findMaxRank();
            color = graduateColorSaturation(color, SSSP.getCHResult().getRanks().get(edge.from), maxRank);
        }
        if (currentOverlay == OverlayType.DENSITY && densityMeasures != null) {
            findMaxDensity();
            color = graduateColorHue(Color.RED, densityMeasures.get(edge.from), maxDensity);
        }
        if (graphInfo != null) {
            EdgeInfo edgeInfo = graphInfo.getEdge(edge);
            NodeInfo nodeInfo = graphInfo.getNodeList().get(edgeInfo.getFrom());
            if (currentOverlay == OverlayType.SPEED_MARKED) {
                if (edgeInfo.getMaxSpeed() == -1) {
                    color = Color.BROWN;
                }
            }
            if (currentOverlay == OverlayType.SPEED_LIMIT) {
                color = graduateColorSaturation(color, edgeInfo.getMaxSpeed(), 140);
            }
            if (currentOverlay == OverlayType.SURFACE) {
                color = getSurfaceColor(edge);
            }
            if (currentOverlay == OverlayType.NATURE_VALUE) {
                if (nodeInfo.getNatureValue() > 0) {
                    color = Color.DARKGREEN;
                    color = graduateColorSaturation(color, nodeInfo.getNatureValue(), 6);
                    color = color.deriveColor(0, 1 - 0.8 + nodeInfo.getNatureValue() / (1.25 * 6), 1, 1);
                }
            }
            if (currentOverlay == OverlayType.FUEL) {
                if (nodeInfo.isFuelAmenity()) {
                    color = Color.CYAN;
                }
            }
        }
        return color;
    }

    private Color getSurfaceColor(Edge edge) {
        EdgeInfo info = graphInfo.getEdge(edge);
        switch (info.getSurface()) {
            case PAVED:
            case CONCRETE:
            case ASPHALT:
                return Color.BLACK;
            case GRAVEL:
                return Color.RED;
            case GROUND:
            case SAND:
            case EARTH:
            case DIRT:
            case UNPAVED:
                return Color.BROWN;
            case GRASS:
                return Color.GREEN;
            case PAVING_STONES:
            case COBBLESTONE:
                return Color.BLUE;
            case UNKNOWN:
            default:
                return Color.PINK;
        }
    }

    private double maxReach = -1;

    private void findMaxReach() {
        if (SSSP.getReachBounds() == null || maxReach != -1) {
            return;
        }
        double maxSoFar = 0;
        for (double reachBound : SSSP.getReachBounds()) {
            if (maxSoFar < reachBound && reachBound < 100000) {
                maxSoFar = reachBound;
            }
        }
        maxReach = maxSoFar;
    }

    double maxRank = -1;

    private void findMaxRank() {
        if (SSSP.getCHResult() == null || maxRank != -1) {
            return;
        }
        double maxSoFar = 0;
        for (double rank : SSSP.getCHResult().getRanks()) {
            if (maxSoFar < rank && rank < 100000) {
                maxSoFar = rank;
            }
        }
        maxRank = maxSoFar;
    }

    private int maxDensity = -1;

    private void findMaxDensity() {
        if (densityMeasures == null || maxDensity != -1) {
            return;
        }
        int maxSoFar = 0;
        for (int density : densityMeasures) {
            if (maxSoFar < density) {
                maxSoFar = density;
            }
        }
        maxDensity = maxSoFar;
    }

    public boolean isBetter(Node from1, Edge e1, Node from2, Edge e2) {
        return compVal(from1, e1) >= compVal(from2, e2);
    }

    private int compVal(Node from, Edge edge) {
        boolean visited = currentResult.relaxedEdgesA.contains(edge);
        boolean reverseVisited = currentResult.relaxedEdgesB.contains(edge.getReverse());
        boolean inPath = currentResult.path.contains(from.index) && currentResult.path.contains(edge.to);
        boolean isDrawn = drawnEdges.contains(edge);
        int compVal = 0;
        if (isDrawn) {
            compVal++;
        }
        if (inPath) {
            compVal++;
        }
        if (visited) {
            compVal++;
        }
        if (reverseVisited) {
            compVal++;
        }
        return compVal;
    }

    private double chooseEdgeWidth(Edge edge) {
        boolean inPath = currentResult.path.contains(edge.from) && currentResult.path.contains(edge.to);
        if (inPath) {
            return 2;
        }
        return 1;
    }

    private Color chooseAlgorithmEdgeColor(Edge edge) {
        boolean visited = currentResult.scannedNodesA.contains(edge.from);
        boolean reverseVisited = currentResult.scannedNodesB.contains(edge.from);
        boolean inPath = currentResult.path.contains(edge.from) && currentResult.path.contains(edge.to);
        if (inPath) {
            int prevNode = currentResult.path.indexOf(edge.from);
            int nextNode = currentResult.path.indexOf(edge.to);
            if (prevNode == nextNode + 1 || prevNode + 1 == nextNode)
                return Color.RED;
            else return Color.BLACK;
        }
        if (reverseVisited) {
            return Color.TURQUOISE;
        }
        if (visited) {
            return Color.BLUE;
        }
        return Color.BLACK;
    }

    private Color graduateColorSaturation(Color color, double amount, double max) {
        return color.deriveColor(-10 * (amount / max), 1, 1, 1 - 0.8 + amount / (1.25 * max));
    }

    private Color graduateColorHue(Color color, double amount, double max) {
        return color.deriveColor(120 * (Math.sqrt(amount) / Math.sqrt(max)), 1, 1, 1);
    }

    private Edge findOppositeEdge(List<List<Edge>> adjList, int i, Edge edge) {
        Edge oppositeEdge = null;
        for (Edge edgeTo : adjList.get(edge.to)) {
            if (edgeTo.to == i) {
                oppositeEdge = edgeTo;
            }
        }
        return oppositeEdge;
    }

    private void drawAllLandmarks() {
        for (Integer index : landmarksGenerator.getLandmarkSet()) {
            Node n = graph.getNodeList().get(index);
            drawLandMark(n);
        }
    }

    private void drawLandMark(Node n) {
        PixelPoint p = toScreenPos(n);
        double radius = 6;
        double shift = radius / 2;
        gc.setFill(Color.HOTPINK);
        gc.setStroke(Color.HOTPINK);
        gc.fillOval(p.x - shift, p.y - shift, radius, radius);
        gc.strokeOval(p.x - shift, p.y - shift, radius, radius);
    }

    // Graph zoom control
    private float zoomFactor;

    private void fitGraph() {
        setGraphBounds();
        zoomFactor = 1;
        setRatios();
        redrawGraph();
    }

    private PixelPoint minXY = new PixelPoint(-1, -1);

    private int widthMeter;
    private int heightMeter;

    private void setGraphBounds() {
        minXY = new PixelPoint(-1, -1);
        PixelPoint maxXY = new PixelPoint(-1, -1);

        List<Node> nodeList = graph.getNodeList();
        for (Node n : nodeList) {
            if (n != null) {
                double x = mercatorX(n.longitude);
                double y = mercatorY(n.latitude);
                minXY.x = (minXY.x == -1) ? x : Math.min(minXY.x, x);
                minXY.y = (minXY.y == -1) ? y : Math.min(minXY.y, y);
            }
        }

        for (Node n : nodeList) {
            if (n != null) {
                double x = mercatorX(n.longitude);
                double y = mercatorY(n.latitude);
                maxXY.x = (maxXY.x == -1) ? x : Math.max(maxXY.x, x);
                maxXY.y = (maxXY.y == -1) ? y : Math.max(maxXY.y, y);
            }
        }
        widthMeter = (int) Math.abs(maxXY.x - minXY.x);
        heightMeter = (int) Math.abs(maxXY.y - minXY.y);
    }

    private double globalRatio;

    private void setRatios() {
        // Determine the width and height ratio because we need to magnify the map to fit into the given image dimension
        double mapWidthRatio = zoomFactor * canvas.getWidth() / widthMeter;
        double mapHeightRatio = zoomFactor * canvas.getHeight() / heightMeter;
        // Using different ratios for width and height will cause the map to be stretched. So, we have to determine
        // the global ratio that will perfectly fit into the given image dimension
        globalRatio = Math.min(mapWidthRatio, mapHeightRatio);
    }

    /**
     * Should be run on GUI thread
     */
    private void setWindowChangeListener() {
        canvas.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                newScene.windowProperty().addListener((observableWindow, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {
                        newScene.heightProperty().addListener((observable, oldValue, newValue) -> {
                            if (graph == null) {
                                return;
                            }
                            canvas.setHeight(newValue.doubleValue());
                            fitGraph();
                        });
                        newScene.widthProperty().addListener((obs, oldVal, newVal) -> {
                            if (graph == null) {
                                return;
                            }
                            canvas.setWidth(newVal.doubleValue());
                            fitGraph();
                        });
                    }
                });
            }
        });
    }

    public void setSceneListeners(Scene scene) {
        scene.setOnKeyPressed(onKeyPressed());
    }

    private int xOffset;
    private int yOffset;

    private void zoom(float v) {
        if (v < 1 && zoomFactor < 0.01 || v > 1 && zoomFactor > 1000) {
            return;
        }
        Node centerNode = toNode(getScreenCenter());
        zoomFactor *= v;
        setRatios();

        PixelPoint oldCenter = toScreenPos(centerNode);
        PixelPoint screenCenter = getScreenCenter();

        xOffset += (screenCenter.x - oldCenter.x) / globalRatio;
        yOffset -= (screenCenter.y - oldCenter.y) / globalRatio;

        redrawGraph();
    }

    private Node selectOutsideGraph(PixelPoint mousePos, Node closestNode) {
        Node mouseNode = toNode(mousePos);
        double dist = distanceStrategy.apply(mouseNode, closestNode);
        graph.addNode(mouseNode);
        Edge forth = new Edge(mouseNode.index, closestNode.index, dist);
        mouseEdges.add(forth);
        graph.addEdge(mouseNode, forth);
        Edge back = new Edge(closestNode.index, mouseNode.index, dist);
        mouseEdges.add(back);
        graph.addEdge(closestNode, back);
        closestNode = mouseNode;
        mouseNodes++;
        return closestNode;
    }

    private void resetSelection() {
        selectedNodes = new ArrayDeque<>();
        graph.removeNodesFromEnd(mouseNodes);
        mouseNodes = 0;
        currentResult = new ShortestPathResult();
        redrawGraph();
    }

    private Node selectClosestNode(PixelPoint p) {
        List<Node> nodeList = graph.getNodeList();
        Node closestNode = nodeList.get(0);
        double closestDistance = nodeToPointDistance(closestNode, p);
        for (Node node : nodeList) {
            double distance = nodeToPointDistance(node, p);
            if (distance < closestDistance) {
                closestNode = node;
                closestDistance = distance;
            }
        }
        return closestNode;
    }

    private PixelPoint getScreenCenter() {
        return new PixelPoint(canvas.getWidth() / 2, canvas.getHeight() / 2);
    }

    // Projections

    private PixelPoint toScreenPos(Node node) {
        return new PixelPoint(toScreenPosX(node.longitude), toScreenPosY(node.latitude));
    }

    private Node toNode(PixelPoint p) {
        return new Node(-1, toLongitude(p.x), toLatitude(p.y));
    }

    final double RADIUS_MAJOR = 6378137.0;

    /**
     * @param cord longitude input
     * @return x coordinate in canvas
     */
    double mercatorX(double cord) {
        return (Math.toRadians(cord) * RADIUS_MAJOR) + xOffset;
    }

    double invMercatorX(double x) {
        return Math.toDegrees((x - xOffset) / RADIUS_MAJOR);
    }

    double toScreenPosX(double longitude) {
        double x = mercatorX(longitude) - minXY.x;
        return x * globalRatio;
    }

    double toLongitude(double x) {
        return invMercatorX(x / globalRatio + minXY.x);
    }

    final double RADIUS_MINOR = 6356752.3142;

    /**
     * @param cord latitude input
     * @return y coordinate in canvas
     */
    double mercatorY(double cord) {
        return (Math.log(Math.tan(Math.PI / 4 + Math.toRadians(cord) / 2)) * RADIUS_MINOR) + yOffset;
    }

    double invMercatorY(double y) {
        return Math.toDegrees(2 * Math.atan(Math.exp((y - yOffset) / RADIUS_MINOR)) - Math.PI / 2);
    }

    double toScreenPosY(double latitude) {
        double y = mercatorY(latitude) - minXY.y;
        return canvas.getHeight() - y * globalRatio;
    }

    double toLatitude(double y) {
        return invMercatorY((canvas.getHeight() - y) / globalRatio + minXY.y);
    }

    double nodeToPointDistance(Node node, PixelPoint p) {
        PixelPoint nodeP = toScreenPos(node);
        return distance(nodeP, p);
    }

    double distance(PixelPoint p1, PixelPoint p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    // Here comes all the eventHandle methods that are called when buttons are clicked
    public void handleNavUpEvent() {
        yOffset -= 0.1 * heightMeter / zoomFactor;
        redrawGraph();
    }

    public void handleNavDownEvent() {
        yOffset += 0.1 * heightMeter / zoomFactor;
        redrawGraph();
    }

    public void handleNavLeftEvent() {
        xOffset += (0.1 * widthMeter) / zoomFactor;
        redrawGraph();
    }

    public void handleNavRightEvent() {
        xOffset -= (0.1 * widthMeter) / zoomFactor;
        redrawGraph();
    }

    public void handleNavCenterEvent() {
        fitGraph();
    }

    public void handleZoomInEvent() {
        if (graph == null) {
            return;
        }
        zoom(1.1f);
    }

    public void handleZoomOutEvent() {
        if (graph == null) {
            return;
        }
        zoom(1 / 1.1f);
    }

    // W A S D navigation
    private EventHandler<? super KeyEvent> onKeyPressed() {
        return event -> {
            if (graph == null) {
                return;
            }
            switch (event.getCode()) {
                case W:
                    handleNavUpEvent();
                    break;
                case A:
                    handleNavLeftEvent();
                    break;
                case S:
                    handleNavDownEvent();
                    break;
                case D:
                    handleNavRightEvent();
                    break;
                case F1:
                    toggleAirDistance();
                    break;
            }
        };
    }

    // Used for calculating how far to drag
    private double clickX = 0, clickY = 0;

    // Used for deciding whether a drag should interrupt a normal click
    private int dragCounter = 0, dragLimit = 5;

    private EventHandler<? super MouseEvent> onMousePressed() {
        return event -> {
            clickX = event.getX();
            clickY = event.getY();
            dragCounter = 0;
        };
    }

    private EventHandler<? super MouseEvent> onMouseDragged() {
        return event -> {
            if (graph == null) {
                return;
            }
            double dx = event.getX() - clickX;
            double dy = clickY - event.getY();
            xOffset += dx / globalRatio;
            yOffset += dy / globalRatio;
            clickX = event.getX();
            clickY = event.getY();
            dragCounter++;
            redrawGraph();
        };
    }

    private EventHandler<MouseEvent> onMouseClicked() {
        return event -> {
            switch (event.getButton()) {
                case PRIMARY:
                    onLeftClick(event);
                    break;
                case MIDDLE:
                    toggleAirDistance();
                    break;
                case SECONDARY:
                    onRightClick();
                    break;
            }
        };
    }

    public boolean includeAirDistance = false;

    private void onLeftClick(MouseEvent event) {
        if (dragCounter > dragLimit || graph == null) {
            return;
        }
        PixelPoint mousePos = new PixelPoint(event.getX(), event.getY());
        Node node = selectClosestNode(mousePos);
        if (includeAirDistance) {
            node = selectOutsideGraph(mousePos, node);
        }
        selectedNodes.addLast(node);
        if (selectedNodes.size() > 1) {
            runAlgorithm();
        } else {
            currentResult = new ShortestPathResult();
            redrawGraph();
        }
    }

    private void toggleAirDistance() {
        includeAirDistance = !includeAirDistance;
    }

    private void onRightClick() {
        if (graph == null) {
            return;
        }
        resetOverlays();
        resetSelection();
        cancelAlgorithm();
    }

    private EventHandler<? super ScrollEvent> onMouseScrolled() {
        return event -> {
            if (event.getEventType() == ScrollEvent.SCROLL) {
                if (event.getDeltaY() < 0) {
                    handleZoomOutEvent();
                } else {
                    handleZoomInEvent();
                }
            }
        };
    }

    public void handleDijkstraEvent() {
        chooseAlgorithm(DIJKSTRA);
    }

    public void handleBiDijkstraEvent() {
        chooseAlgorithm(BI_DIJKSTRA);
    }

    public void handleBiDijkstraSameDistEvent() {
        chooseAlgorithm(BI_DIJKSTRA_SAME_DIST);
    }

    public void handleBiDijkstraDensityEvent() {
        chooseAlgorithm(BI_DIJKSTRA_DENSITY);
    }

    public void handleAStarEvent() {
        chooseAlgorithm(A_STAR);
    }

    public void handleBiAStarConEvent() {
        chooseAlgorithm(BI_A_STAR_CONSISTENT);
    }

    public void handleBiAStarSymEvent() {
        chooseAlgorithm(BI_A_STAR_SYMMETRIC);
    }

    public void handleLandmarksEvent() {
        chooseAlgorithm(A_STAR_LANDMARKS);
    }

    public void handleBiLandmarksEvent() {
        chooseAlgorithm(BI_A_STAR_LANDMARKS);
    }

    private void chooseAlgorithm(AlgorithmMode algorithmMode) {
        if (currentOverlay == OverlayType.REACH) {
            currentOverlay = OverlayType.NONE;
        }
        this.algorithmMode = algorithmMode;
        runAlgorithm();
        setAlgorithmLabels();
    }

    public void handleSeedEvent() {
        seed++;
        int n = graph.getNodeAmount();
        Random random = new Random(seed);
        selectedNodes = new ArrayDeque<>();
        selectedNodes.add(graph.getNodeList().get(random.nextInt(n)));
        selectedNodes.add(graph.getNodeList().get(random.nextInt(n)));
        runAlgorithm();
        setSeedLabel();
    }

    public void handleChooseFileEvent() {
        selectedNodes = new ArrayDeque<>();
        File selectedFile = GraphIO.selectMapFile(stage);
        if (selectedFile != null) {
            loadNewGraph(selectedFile.getName());
        }
    }

    public void handleGenerateLandmarksAvoid() {
        Landmarks lm = new Landmarks(graph);
        landmarksGenMode = LandmarkMode.AVOID;
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getAvoidFunction(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);
    }

    public void handleGenerateLandmarksMaxCover() {
        Landmarks lm = new Landmarks(graph);
        landmarksGenMode = LandmarkMode.MAXCOVER;
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getMaxCover(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);
    }

    public void handleGenerateLandmarksRandom() {
        Landmarks lm = new Landmarks(graph);
        landmarksGenMode = LandmarkMode.RANDOM;
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getRandomFunction(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);

    }

    public void handleGenerateLandmarksFarthest() {
        Landmarks lm = new Landmarks(graph);
        landmarksGenMode = LandmarkMode.FARTHEST;
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getFarthestFunction(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);
    }

    public void handleLoadLandmarks() {
        pickLandMarkModeGUI();
        new GraphIO(distanceStrategy, isSCCGraph).loadLandmarks(fileName, landmarksGenMode, landmarksGenerator);
        redrawGraph();
    }

    private void pickLandMarkModeGUI() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Landmark Generation");
        alert.setHeaderText("Pick a Mode of Landmark Generation");
        alert.setContentText("Choose your option.");

        ButtonType buttonTypeOne = new ButtonType("Avoid");
        ButtonType buttonTypeTwo = new ButtonType("MaxCover");
        ButtonType buttonTypeThree = new ButtonType("Farthest");
        ButtonType buttonTypeFour = new ButtonType("Random");


        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo, buttonTypeThree, buttonTypeFour);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOne) {
            // ... user chose "One"
            landmarksGenMode = LandmarkMode.AVOID;
        } else if (result.get() == buttonTypeTwo) {
            // ... user chose "Two"
            landmarksGenMode = LandmarkMode.MAXCOVER;
        } else if (result.get() == buttonTypeThree) {
            // ... user chose "Three"
            landmarksGenMode = LandmarkMode.FARTHEST;
        } else {
            landmarksGenMode = LandmarkMode.RANDOM;
            // ... user chose CANCEL or closed the dialog
        }
    }

    public void handleSaveLandmarks() {
        new GraphIO(distanceStrategy, isSCCGraph).saveLandmarks(fileName, landmarksGenMode, landmarksGenerator.getLandmarkSet());
    }

    public void handleSetParameterEvent() {
        displayParameterDialog();
    }

    public void handleSCCEvent() {
        if (!isSCCGraph) {
            runSCC();
        } else {
            System.out.println("SCC already found");
        }
    }

    public void handleLoadInfo() {
        parseInfo();
    }

    public void handleReachEvent() {
        chooseReachAlgorithm(REACH);
    }

    public void handleBiReachEvent() {
        chooseReachAlgorithm(BI_REACH);
    }

    public void handleReachAStarEvent() {
        chooseReachAlgorithm(REACH_A_STAR);
    }

    public void handleBiReachAStarEvent() {
        chooseReachAlgorithm(BI_REACH_A_STAR);
    }

    public void handleReachLandmarksEvent() {
        chooseReachAlgorithm(REACH_LANDMARKS);
    }

    public void handleBiReachLandmarksEvent() {
        chooseReachAlgorithm(BI_REACH_LANDMARKS);
    }

    private void chooseReachAlgorithm(AlgorithmMode mode) {
        if (SSSP.getReachBounds() == null) {
            System.out.println("No reach bounds found. Trying to load them.");
            loadReachBounds();
            return;
        }
        chooseAlgorithm(mode);
        currentOverlay = OverlayType.REACH;
    }

    public void handleGenerateReachEvent() {
        generateReachBounds();
    }

    public void handleCHEvent() {
        chooseAlgorithm(CONTRACTION_HIERARCHIES);
    }

    public void handleGenerateCHEvent() {
        GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
        if (!graphIO.fileExtensionExists(fileName, "-contraction-hierarchies.tmp")) {
            generateContractionHierarchies();
        }
    }

    private void generateContractionHierarchies() {
        System.out.println("Generating CH graph!");
        maxRank = -1;
        Task<CHResult> CHTask = new Task<>() {
            @Override
            protected CHResult call() {
                ContractionHierarchies contractionHierarchies = new ContractionHierarchies(graph);
                contractionHierarchies.setProgressListener(this::updateProgress);
                return contractionHierarchies.preprocess();
            }
        };
        CHTask.setOnSucceeded(e -> {
            try {
                SSSP.setCHResult(CHTask.get());
                System.out.println("CH is generated successfully!");
                saveContractionHierarchies();
                playIndicatorCompleted();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        });
        CHTask.setOnFailed(e -> {
            playIndicatorCompleted();
            displayFailedDialog("generate contraction hierarchies", e);
        });
        attachProgressIndicator(CHTask.progressProperty());
        new Thread(CHTask).start();
    }

    private void loadContractionHierarchies() {
        Task<CHResult> loadTask = new Task<>() {
            @Override
            protected CHResult call() {
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                return graphIO.loadCH(fileName);
            }
        };
        loadTask.setOnSucceeded(e -> {
            SSSP.setCHResult(loadTask.getValue());
        });
        loadTask.setOnFailed(e -> displayFailedDialog("load contraction hierarchies", e));
        new Thread(loadTask).start();
    }

    private void saveContractionHierarchies() {
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                graphIO.saveCH(fileName, SSSP.getCHResult());
                System.out.println("Saved Contraction Hierarchies successfully!");
                return null;
            }
        };
        saveTask.setOnFailed(e -> displayFailedDialog("save contraction hierarchies", e));
        new Thread(saveTask).start();
    }

    // UTILITIES
    private void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setLabels(String distance, int totalVisitedNodes) {
        setAlgorithmLabels();

        distance_label.setText("Distance (km): " + distance);
        nodes_visited_label.setText("Nodes Visited: " + totalVisitedNodes);
    }

    private void setAlgorithmLabels() {
        algorithm_label.setText("Algorithm: " + algorithmNames.get(algorithmMode));
        source_label.setText("Source: " + SSSP.getSource());
        target_label.setText("Target: " + SSSP.getTarget());
        setSeedLabel();
    }

    private void setSeedLabel() {
        seed_label.setText("Seed: " + SSSP.seed);
    }


    private Task<Void> generateLandmarksTask(BiFunction<Integer, Boolean, Set<Integer>> landmarksFunction, Landmarks lm, int goalAmount) {
        return new Task<>() {
            @Override
            protected Void call() {
                lm.setProgressListener(this::updateProgress);
                landmarksFunction.apply(goalAmount, false);
                landmarksGenerator = lm;
                return null;
            }
        };
    }

    private void startLandmarksMonitorThread(Task<Void> monitorTask) {
        monitorTask.setOnSucceeded(event -> {
            redrawGraph();
            SSSP.setLandmarks(landmarksGenerator);
            new GraphIO(distanceStrategy, isSCCGraph).saveLandmarks(fileName, landmarksGenMode, landmarksGenerator.getLandmarkSet());
            playIndicatorCompleted();
        });
        attachProgressIndicator(monitorTask.progressProperty());
        new Thread(monitorTask).start();
    }

    // https://code.makery.ch/blog/javafx-dialogs-official/
    private void displayParameterDialog() {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Set your parameters");

        // Set the button types.
        ButtonType acceptButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptButton, ButtonType.CANCEL);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(acceptButton);
        okButton.setDisable(true);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 10, 10, 10));


        NumericTextField from = new NumericTextField();
        from.setPromptText("From");
        NumericTextField to = new NumericTextField();
        to.setPromptText("To");
        NumericTextField seed = new NumericTextField();
        seed.setPromptText("Seed");

        from.textProperty().addListener((observable, oldValue, newValue) ->
                okButton.setDisable(Integer.parseInt(newValue) > graph.getNodeList().size()));

        to.textProperty().addListener((observable, oldValue, newValue) ->
                okButton.setDisable(Integer.parseInt(newValue) > graph.getNodeList().size()));

        gridPane.add(new Label("From:"), 0, 0);
        gridPane.add(from, 1, 0);
        gridPane.add(new Label("To:"), 2, 0);
        gridPane.add(to, 3, 0);
        gridPane.add(new Label("Seed:"), 0, 1);
        gridPane.add(seed, 1, 1);

        dialog.getDialogPane().setContent(gridPane);

        // Request focus on the username field by default.
        Platform.runLater(from::requestFocus);

        // Convert the result to wanted information.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == acceptButton) {
                onRightClick();
                if (seed.getText().equals("")) {
                    SSSP.seed = 0;
                } else {
                    SSSP.seed = Integer.parseInt(seed.getText());
                }
                setSeedLabel();
                List<Node> nodeList = graph.getNodeList();
                selectedNodes.add(nodeList.get(Integer.parseInt(from.getText())));
                selectedNodes.add(nodeList.get(Integer.parseInt(to.getText())));
                runAlgorithm();
            }
            return null;
        });
        dialog.show();
    }

    private void displayFailedDialog(String taskName, WorkerStateEvent event) {
        try {
            throw event.getSource().getException();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Task failed");
        dialog.setContentText("Task: " + taskName + " failed to finish");
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        dialog.show();
    }

    private void displayGetNodesDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Input node pairs");
        dialog.setContentText("Input node pairs here: ");
        TextField textField = new TextField();
        textField.setPromptText("Node pairs");
        dialog.getDialogPane().setContent(textField);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okButton);
        dialog.setResultConverter(buttonType -> textField.getText());
        dialog.showAndWait();
        for (String res : dialog.getResult().split("[)]")) {
            if (res.length() < 1) {
                return;
            }
            String[] pair = res.replaceAll(" ", "").substring(1).split(",");
            int fromIndex = Integer.parseInt(pair[0]);
            int toIndex = Integer.parseInt(pair[1]);
            overlayNodes.add(graph.getNodeList().get(fromIndex));
            overlayNodes2.add(graph.getNodeList().get(toIndex));
        }
    }
    private float displayGetRadiusDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Input radius");
        dialog.setContentText("Input radius here: ");
        TextField textField = new TextField();
        textField.setPromptText("radius");
        dialog.getDialogPane().setContent(textField);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okButton);
        dialog.setResultConverter(buttonType -> textField.getText());
        dialog.showAndWait();
        return Float.parseFloat(dialog.getResult());
    }

    public void handleClearLandmarks() {
        landmarksGenerator.clearLandmarks();
        SSSP.setLandmarkArray(null);
        redrawGraph();
    }

    private void generateReachBounds() {
        maxReach = -1;
        FXMLController f = this;
        Task<List<Double>> reachGenTask = new Task<>() {
            @Override
            protected List<Double> call() {
                ReachProcessor reachProcessor = new ReachProcessor();
                reachProcessor.setProgressListener(this::updateProgress);
                reachProcessor.SetFXML(f);
                return reachProcessor.computeReachBound(new Graph(graph));
            }
        };
        reachGenTask.setOnSucceeded(e -> {
            List<Double> bounds = reachGenTask.getValue();
            SSSP.setReachBounds(bounds);
            saveReachBounds();
            playIndicatorCompleted();
        });
        reachGenTask.setOnFailed(e -> {
            playIndicatorCompleted();
            displayFailedDialog("generate reach bounds", e);
        });
        attachProgressIndicator(reachGenTask.progressProperty());
        new Thread(reachGenTask).start();
    }


    private void loadReachBounds() {
        Task<List<Double>> loadTask = new Task<>() {
            @Override
            protected List<Double> call() {
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                return graphIO.loadReach(fileName);
            }
        };
        loadTask.setOnSucceeded(e -> {
            SSSP.setReachBounds(loadTask.getValue());
            findMaxReach();
        });
        loadTask.setOnFailed(e -> displayFailedDialog("load reach bounds", e));
        new Thread(loadTask).start();
    }

    private void saveReachBounds() {
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                GraphIO graphIO = new GraphIO(isSCCGraph);
                graphIO.saveReach(fileName, SSSP.getReachBounds());
                return null;
            }
        };
        saveTask.setOnFailed(e -> displayFailedDialog("save reach bounds", e));
        new Thread(saveTask).start();
    }

    public void handleGenerateDensities() {
        float radius = displayGetRadiusDialog();
        Task<List<Integer>> generateDensitiesTask = new Task<>() {
            @Override
            protected List<Integer> call() {
                ModelUtil modelUtil = new ModelUtil(graph);
                modelUtil.setProgressListener(this::updateProgress);
                return modelUtil.computeDensityMeasures(radius);
            }
        };
        generateDensitiesTask.setOnSucceeded(e -> {
            densityMeasures = generateDensitiesTask.getValue();
            SSSP.setDensityMeasures(densityMeasures);
            saveDensities();
            playIndicatorCompleted();
        });
        generateDensitiesTask.setOnFailed(e -> {
            displayFailedDialog("generate densities", e);
            playIndicatorCompleted();
        });
        attachProgressIndicator(generateDensitiesTask.progressProperty());
        new Thread(generateDensitiesTask).start();
    }

    public void loadDensities() {
        Task<List<Integer>> loadTask = new Task<>() {
            @Override
            protected List<Integer> call() {
                GraphIO graphIO = new GraphIO(distanceStrategy, isSCCGraph);
                return graphIO.loadDensities(fileName);
            }
        };
        loadTask.setOnSucceeded(e -> {
            findMaxDensity();
            densityMeasures = loadTask.getValue();
            SSSP.setDensityMeasures(densityMeasures);
        });
        loadTask.setOnFailed(e -> displayFailedDialog("load densities", e));
        new Thread(loadTask).start();
    }

    public void saveDensities() {
        Task<Void> saveDensitiesTask = new Task<>() {
            @Override
            protected Void call() {
                new GraphIO(isSCCGraph).storeDensities(fileName, densityMeasures);
                return null;
            }
        };
        saveDensitiesTask.setOnFailed(e -> displayFailedDialog("save densities", e));
        new Thread(saveDensitiesTask).start();
    }

    private void runSCC() {
        System.out.println("Computing SCC");
        Task<GraphPair> sccTask = new Task<>() {
            @Override
            protected GraphPair call() {
                ModelUtil gu = new ModelUtil(graph);
                gu.setProgressListener(this::updateProgress);
                List<Integer> nodesToKeep = gu.scc().get(0);
                if (graphInfo == null) {
                    return new GraphPair(gu.subGraph(nodesToKeep), null);
                }
                updateProgress(99L, 100L);
                return gu.subGraphPair(graphInfo, nodesToKeep);
            }
        };
        sccTask.setOnSucceeded(e -> {
            graph = sccTask.getValue().getGraph();
            graphInfo = sccTask.getValue().getGraphInfo();
            isSCCGraph = true;
            storeGraph();
            setUpGraph();
            System.out.println("Finished computing SCC");
            playIndicatorCompleted();
        });
        sccTask.setOnFailed(e -> {
            playIndicatorCompleted();
            displayFailedDialog("compute SCC", e);
        });
        attachProgressIndicator(sccTask.progressProperty());
        new Thread(sccTask).start();
    }

    private Timeline indicatorTimeline;

    private void attachProgressIndicator(ReadOnlyDoubleProperty progressProperty) {
        if (indicatorTimeline != null) {
            indicatorTimeline.stop();
        }
        progress_indicator.setOpacity(1);
        progress_indicator.progressProperty().bind(progressProperty);
    }

    private void playIndicatorCompleted() {
        indicatorTimeline = new Timeline(new KeyFrame(Duration.millis(10), actionEvent -> {
            progress_indicator.setOpacity(progress_indicator.getOpacity() - 0.005);
        }));
        indicatorTimeline.setCycleCount(200);
        indicatorTimeline.playFromStart();
    }

    public void handleOverlayNone() {
        currentOverlay = OverlayType.NONE;
        redrawGraph();
    }

    public void handleOverlayReach() {
        currentOverlay = OverlayType.REACH;
        redrawGraph();
    }

    public void handleOverlayCH() {
        currentOverlay = OverlayType.CH;
        redrawGraph();
    }

    public void handleOverlayNodes() {
        currentOverlay = OverlayType.NODEPAIRS;
        displayGetNodesDialog();
        redrawGraph();
    }

    public void handleOverlayDensity() {
        currentOverlay = OverlayType.DENSITY;
        redrawGraph();
    }

    public void handleOverlaySpeed() {
        currentOverlay = OverlayType.SPEED_MARKED;
        redrawGraph();
    }

    public void handleOverlaySpeedLimits() {
        currentOverlay = OverlayType.SPEED_LIMIT;
        redrawGraph();
    }

    public void handleOverlaySurface() {
        currentOverlay = OverlayType.SURFACE;
        redrawGraph();
    }

    public void handleOverlayNature() {
        currentOverlay = OverlayType.NATURE_VALUE;
        redrawGraph();
    }

    public void handleOverlayFuel() {
        currentOverlay = OverlayType.FUEL;
        redrawGraph();
    }

    public void handleWeightDistance() {
        SSSP.setEdgeWeightStrategy(EdgeWeightGenerator.getDistanceWeights());
        runAlgorithm();
    }

    public void handleWeightSpeed() {
        if (graphInfo != null) {
            SSSP.setEdgeWeightStrategy(EdgeWeightGenerator.getMaxSpeedTime());
            runAlgorithm();
        } else {
            System.out.println("Info hasn't been generated for this map: " + fileName);
        }
    }

    public void handleWeightFlat() {
        System.out.println("Not implemented");
    }

    public void handleWeightTrees() {
        System.out.println("Not implemented");
    }
}
