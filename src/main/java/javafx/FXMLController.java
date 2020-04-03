package javafx;

// TODO: toggle graphical info about landmarks

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
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
import load.GraphImport;
import model.Edge;
import model.Graph;
import model.Node;
import model.Util;
import paths.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static model.Util.algorithmNames;
import static paths.AlgorithmMode.*;
import static paths.SSSP.seed;

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
    private Graph graph;
    private Landmarks landmarksGenerator;
    private GraphicsContext gc;

    private int xOffset;
    private int yOffset;

    private PixelPoint minXY = new PixelPoint(-1, -1);
    private PixelPoint maxXY = new PixelPoint(-1, -1);

    private double globalRatio;
    private double mapWidthRatio;
    private double mapHeightRatio;

    private float zoomFactor;
    private int widthOfBoundingBox;
    private int heightOfBoundingBox;

    private BiFunction<Node, Node, Double> distanceStrategy;
    private AlgorithmMode algorithmMode = DIJKSTRA;
    private Deque<Node> selectedNodes = new ArrayDeque<>();
    private int mouseNodes = 0;
    private String fileName;

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
        this.fileName = fileName;
        Task<Graph> loadGraphTask = new Task<>() {
            @Override
            protected Graph call() {
                GraphImport graphImport = new GraphImport(distanceStrategy);
                graphImport.setProgressListener(this::updateProgress);
                return graphImport.loadGraph(fileName);
            }
        };
        loadGraphTask.setOnSucceeded(event -> {
            graph = loadGraphTask.getValue();
            landmarksGenerator = new Landmarks(graph);
            setUpGraph();
            playIndicatorCompleted();
        });
        attachProgressIndicator(loadGraphTask.progressProperty());
        new Thread(loadGraphTask).start();
    }

    private void playIndicatorCompleted() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(10), actionEvent -> {
            progress_indicator.setOpacity(progress_indicator.getOpacity() - 0.005);
        }));
        timeline.setCycleCount(200);
        timeline.playFromStart();
    }

    /**
     * Is called on the GUI thread when a loadGraph thread is finished.
     */
    private void setUpGraph() {
        if (gc != null) {
            nodes_label.setText("Number of Nodes: " + graph.getNodeAmount());
            edges_label.setText("Number of Edges: " + graph.getEdgeAmount());
        }
        fitGraph();
        SSSP.setGraph(graph);
        SSSP.setLandmarks(landmarksGenerator);
    }

    private Task<List<ShortestPathResult>> ssspTask;

    private void runAlgorithm() {
        if (selectedNodes.size() <= 1) {
            return;
        }
        graph.resetPathTrace();
        ssspTask = new Task<>() {
            @Override
            protected List<ShortestPathResult> call() {
                return ssspConnectingNodes(new ArrayDeque<>(selectedNodes));
            }
        };
        ssspTask.setOnSucceeded(event -> {
            applyResultsToLabel(ssspTask.getValue());
            redrawGraph();
        });
        ssspTask.run();
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

    private void applyResultsToLabel(List<ShortestPathResult> results) {
        Optional<ShortestPathResult> optCombinedRes = results.stream().reduce((res1, res2) -> {
            List<Integer> combinedPath = new ArrayList<>(res1.path);
            combinedPath.addAll(res2.path.subList(1, res2.path.size()));
            return new ShortestPathResult(res1.d + res2.d, combinedPath, res1.visitedNodes + res2.visitedNodes, res1.runTime + res2.runTime);
        });
        if (optCombinedRes.isPresent()) {
            ShortestPathResult combinedRes = optCombinedRes.get();
            setLabels(Util.roundDouble(combinedRes.d), combinedRes.visitedNodes);
        }
    }

    private void cancelAlgorithm() {
        if (ssspTask != null) {
            ssspTask.cancel();
        }
    }

    private void setRatios() {
        // Determine the width and height ratio because we need to magnify the map to fit into the given image dimension
        mapWidthRatio = zoomFactor * canvas.getWidth() / maxXY.x;
        mapHeightRatio = zoomFactor * canvas.getHeight() / maxXY.y;
        // Using different ratios for width and height will cause the map to be stretched. So, we have to determine
        // the global ratio that will perfectly fit into the given image dimension
        globalRatio = Math.min(mapWidthRatio, mapHeightRatio);
    }

    /**
     * The main method that draws the Graph inside the Canvas. It iterates through all
     * the nodes and draw the Edges.
     */
    private void redrawGraph() {
        clearCanvas();
        drawAllEdges();
        drawSelectedNodes();
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

    private void drawNode(Node node) {
        PixelPoint np = toScreenPos(node);
        double radius = 6;
        double shift = radius / 2;
        gc.fillOval(np.x - shift, np.y - shift, radius, radius);
        gc.strokeOval(np.x - shift, np.y - shift, radius, radius);
    }

    private void drawAllEdges() {
        List<Node> nodeList = graph.getNodeList();
        List<List<Edge>> adjList = graph.getAdjList();
        // Resets the 'isDrawn' property inside Edges.
        resetIsDrawn(adjList);
        // Iterates through all nodes
        for (int i = 0; i < adjList.size(); i++) {
            Node from = nodeList.get(i);
            // Iterates through adjacent nodes/edges
            for (Edge edge : adjList.get(i)) {
                Node to = nodeList.get(edge.to);
                Edge oppositeEdge = findOppositeEdge(adjList, i, edge);
                // If the edge doesn't violate these constrains it will be drawn in the Canvas.
                if (oppositeEdge == null || edge.isBetter(oppositeEdge)) {
                    PixelPoint pFrom = toScreenPos(from);
                    PixelPoint pTo = toScreenPos(to);

                    gc.setStroke(chooseEdgeColor(edge));
                    gc.setLineWidth(chooseEdgeWidth(edge));
                    if (edge.mouseEdge) {
                        gc.setLineDashes(7);
                    } else {
                        gc.setLineDashes(0);
                    }
                    gc.strokeLine(pFrom.x, pFrom.y, pTo.x, pTo.y);
                    edge.isDrawn = true;
                }
            }
        }
    }

    private double chooseEdgeWidth(Edge edge) {
        if (edge.inPath) {
            return 2;
        }
        return 1;
    }

    private Color chooseEdgeColor(Edge edge) {
        if (edge.inPath) {
            return Color.RED;
        }
        if (edge.visitedReverse) {
            return Color.TURQUOISE;
        }
        if (edge.visited) {
            return Color.BLUE;
        }
        return Color.BLACK;
    }

    private Color shiftColorByRound(Color color, int roundVisit, int totalrounds) {
        double scaleFactor = Math.max(((double) roundVisit / (double) totalrounds), 0.3);
        return Color.hsb(color.getHue(), color.getSaturation(), color.getBrightness() * scaleFactor, color.getOpacity());
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
        double radius = 10;
        double shift = radius / 2;
        gc.setFill(Color.HOTPINK);
        gc.setStroke(Color.HOTPINK);
        gc.fillOval(p.x - shift, p.y - shift, radius, radius);
        gc.strokeOval(p.x - shift, p.y - shift, radius, radius);
    }

    private void resetIsDrawn(List<List<Edge>> adjList) {
        for (List<Edge> edgeList : adjList) {
            for (Edge edge : edgeList) {
                edge.isDrawn = false;
            }
        }
    }

    // Projections

    PixelPoint toScreenPos(Node node) {
        return new PixelPoint(toScreenPosX(node.longitude), toScreenPosY(node.latitude));
    }

    Node toNode(PixelPoint p) {
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

    // Graph zoom control

    private void fitGraph() {
        setGraphBounds();
        zoomFactor = 1;
        setRatios();
        redrawGraph();
    }

    private void setGraphBounds() {
        minXY = new PixelPoint(-1, -1);
        maxXY = new PixelPoint(-1, -1);

        List<Node> nodeList = graph.getNodeList();
        for (Node n : nodeList) {
            double x = mercatorX(n.longitude);
            double y = mercatorY(n.latitude);
            minXY.x = (minXY.x == -1) ? x : Math.min(minXY.x, x);
            minXY.y = (minXY.y == -1) ? y : Math.min(minXY.y, y);
        }

        for (Node n : nodeList) {
            double x = mercatorX(n.longitude) - minXY.x;
            double y = mercatorY(n.latitude) - minXY.y;
            maxXY.x = (maxXY.x == -1) ? x : Math.max(maxXY.x, x);
            maxXY.y = (maxXY.y == -1) ? y : Math.max(maxXY.y, y);
        }
        widthOfBoundingBox = (int) Math.abs(maxXY.x - minXY.x);
        heightOfBoundingBox = (int) Math.abs(maxXY.y - minXY.y);
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

    private double magicConstant = 50;

    private void zoom(float v) {
        Node centerNode = toNode(getScreenCenter());
        zoomFactor *= v;
        setRatios();

        PixelPoint oldCenter = toScreenPos(centerNode);
        PixelPoint screenCenter = getScreenCenter();
        // TODO: Fix magic factor??
        double magicFactor = magicConstant / zoomFactor;
        xOffset += magicFactor * (screenCenter.x - oldCenter.x);
        yOffset -= magicFactor * (screenCenter.y - oldCenter.y);

        redrawGraph();
    }

    private Node selectOutsideGraph(PixelPoint mousePos, Node closestNode) {
        Node mouseNode = toNode(mousePos);
        double dist = distanceStrategy.apply(mouseNode, closestNode);
        graph.addNode(mouseNode);
        Edge forth = new Edge(closestNode.index, dist);
        forth.mouseEdge = true;
        graph.addEdge(mouseNode, forth);
        Edge back = new Edge(mouseNode.index, dist);
        back.mouseEdge = true;
        graph.addEdge(closestNode, back);
        closestNode = mouseNode;
        mouseNodes++;
        return closestNode;
    }

    private void resetSelection() {
        selectedNodes = new ArrayDeque<>();
        graph.removeNodesFromEnd(mouseNodes);
        mouseNodes = 0;
        graph.resetPathTrace();
        redrawGraph();
    }

    // Here comes all the eventHandle methods that are called when buttons are clicked
    public void handleNavUpEvent() {
        yOffset -= (zoomFactor <= 1) ? ((0.1 * heightOfBoundingBox * mapHeightRatio) / zoomFactor) :
                ((0.1 * heightOfBoundingBox * mapHeightRatio) / (2.5 * zoomFactor));
        redrawGraph();
    }

    public void handleNavDownEvent() {
        yOffset += (zoomFactor <= 1) ? ((0.1 * heightOfBoundingBox * mapHeightRatio) / zoomFactor) :
                ((0.1 * heightOfBoundingBox * mapHeightRatio) / (2.5 * zoomFactor));
        redrawGraph();
    }

    public void handleNavLeftEvent() {
        xOffset += (zoomFactor <= 1) ? ((0.1 * widthOfBoundingBox * mapWidthRatio) / zoomFactor) :
                ((0.1 * widthOfBoundingBox * mapWidthRatio) / (2.5 * zoomFactor));
        redrawGraph();
    }

    public void handleNavRightEvent() {
        xOffset -= (zoomFactor <= 1) ? ((0.1 * widthOfBoundingBox * mapWidthRatio) / zoomFactor) :
                ((0.1 * widthOfBoundingBox * mapWidthRatio) / (2.5 * zoomFactor));
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
    // TODO: Find out how arrow keys are triggered
    private EventHandler<? super KeyEvent> onKeyPressed() {
        return event -> {
            if (graph == null) {
                return;
            }
            switch (event.getCode()) {
                case W:
                case UP:
                    handleNavUpEvent();
                    break;
                case A:
                case LEFT:
                    handleNavLeftEvent();
                    break;
                case S:
                case DOWN:
                    handleNavDownEvent();
                    break;
                case D:
                case RIGHT:
                    handleNavRightEvent();
                    break;
                case SHIFT:
                    toggleAirDistance();
                    break;
                case M:
                    magicConstant *= 1.5;
                    System.out.println("Magic constant: " + magicConstant);
                    break;
                case N:
                    magicConstant *= 0.5;
                    System.out.println("Magic constant: " + magicConstant);
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
            // TODO: Make completely smooth by doing reverse mercator
            double magicFactor = magicConstant / zoomFactor;
            double dx = event.getX() - clickX;
            double dy = clickY - event.getY();
            xOffset += magicFactor * dx;
            yOffset += magicFactor * dy;
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
            graph.resetPathTrace();
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
        algorithmMode = DIJKSTRA;
        runAlgorithm();
        setAlgorithmLabels();
    }

    public void handleBiDijkstraEvent() {
        algorithmMode = BI_DIJKSTRA;
        runAlgorithm();
        setAlgorithmLabels();
    }

    public void handleAStarEvent() {
        algorithmMode = A_STAR;
        runAlgorithm();
        setAlgorithmLabels();
    }

    public void handleBiAStarConEvent() {
        algorithmMode = BI_A_STAR_CONSISTENT;
        runAlgorithm();
        setAlgorithmLabels();
    }

    public void handleBiAStarSymEvent() {
        algorithmMode = BI_A_STAR_SYMMETRIC;
        runAlgorithm();
        setAlgorithmLabels();
    }

    public void handleLandmarksEvent() {
        algorithmMode = A_STAR_LANDMARKS;
        runAlgorithm();
        setAlgorithmLabels();
    }

    public void handleBiLandmarksEvent() {
        algorithmMode = BI_A_STAR_LANDMARKS;
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
        File selectedFile = GraphImport.selectMapFile(stage);
        loadNewGraph(selectedFile.getName());
    }

    public void handleGenerateLandmarksAvoid() {
        Landmarks lm = new Landmarks(graph);
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getAvoidFunction(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);
    }

    public void handleGenerateLandmarksMaxCover() {
        Landmarks lm = new Landmarks(graph);
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getMaxCover(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);
    }

    public void handleGenerateLandmarksRandom() {
        Landmarks lm = new Landmarks(graph);
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getRandomFunction(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);
    }

    public void handleGenerateLandmarksFarthest() {
        Landmarks lm = new Landmarks(graph);
        Task<Void> genLandmarkTask = generateLandmarksTask(lm.getFarthestFunction(lm), lm, 16);
        startLandmarksMonitorThread(genLandmarkTask);
    }

    public void handleLoadLandmarks() {
        try {
            GraphImport.loadLandmarks(fileName, landmarksGenerator);
            drawAllLandmarks();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleSaveLandmarks() {
        try {
            String name = GraphImport.tempDir + fileName.substring(0, fileName.indexOf('.'));
            FileOutputStream fos = new FileOutputStream(name + "-landmarks.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(landmarksGenerator.getLandmarkSet());
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleSetParameterEvent() {
        displayParameterDialog();
    }

    public void handleSCCEvent() {
        runSCC();
    }

    // UTILITIES
    private void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setLabels(String distance, int visitedNodes) {
        setAlgorithmLabels();

        distance_label.setText("Total Distance: " + distance);
        nodes_visited_label.setText("Nodes Visited: " + visitedNodes);
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
            drawAllLandmarks();
            SSSP.setLandmarks(landmarksGenerator);
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

    public void handleClearLandmarks() {
        landmarksGenerator.clearLandmarks();
        redrawGraph();
    }

    private void runSCC() {
        Task<List<Graph>> sccTask = new Task<>() {
            @Override
            protected List<Graph> call() {
                GraphUtil gu = new GraphUtil(graph);
                gu.setProgressListener(this::updateProgress);
                List<Graph> graphs = gu.scc();
                updateProgress(100L, 100L);
                return graphs;
            }
        };
        sccTask.setOnSucceeded(e -> {
            playIndicatorCompleted();
            List<Graph> subGraphs = sccTask.getValue().stream().filter(g -> g.getNodeAmount() > 2).collect(Collectors.toList());
            graph = subGraphs.get(0);
            setUpGraph();
        });
        attachProgressIndicator(sccTask.progressProperty());
        sccTask.run();
    }

    private void attachProgressIndicator(ReadOnlyDoubleProperty progressProperty) {
        progress_indicator.setOpacity(1);
        progress_indicator.progressProperty().bind(progressProperty);
    }
}
