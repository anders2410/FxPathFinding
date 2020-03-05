package javafx;

import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Edge;
import model.Graph;
import model.Node;
import model.Util;
import paths.AlgorithmMode;
import paths.Dijkstra;
import paths.ShortestPathResult;
import pbfparsing.PBFParser;
import xml.XMLFilter;
import xml.XMLGraphExtractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.*;
import java.util.function.BiFunction;

import static model.Util.algorithmNames;
import static paths.AlgorithmMode.*;
import static paths.Dijkstra.seed;

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
    private Label seed_label;
    @FXML
    private Button dijkstraButton;
    @FXML
    private Button biDijkstraButton;
    @FXML
    private Button aStarButton;
    @FXML
    private Button biAStarButton;

    private Stage stage;
    private Graph graph;
    private GraphicsContext gc;
    private int xOffset;
    private int yOffset;
    private PixelPoint minXY;
    private PixelPoint maxXY;
    private double globalRatio;
    private float zoomFactor;
    private int widthOfBoundingBox;
    private int heightOfBoundingBox;
    private double mapWidthRatio;
    private double mapHeightRatio;
    private BiFunction<Node, Node, Double> distanceStrategy;
    private AlgorithmMode algorithmMode = DIJKSTRA;
    private Deque<Node> selectedNodes = new ArrayDeque<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        distanceStrategy = Util::sphericalDistance;
        canvas.setOnMouseClicked(onMouseClicked());
        canvas.setOnMousePressed(onMousePressed());
        canvas.setOnMouseDragged(onMouseDragged());
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);
        setUpNewGraph("malta-latest.osm.pbf");
        Dijkstra.setDistanceStrategy(distanceStrategy);
        setSeedLabel();
    }

    public void setSceneListeners(Scene scene) {
        scene.setOnKeyPressed(onKeyPressed());
    }

    private void setUpNewGraph(String fileName) {
        loadGraph(fileName);
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
            double x = projectXCordMercator(n.longitude);
            double y = projectYCordMercator(n.latitude);
            minXY.x = (minXY.x == -1) ? x : Math.min(minXY.x, x);
            minXY.y = (minXY.y == -1) ? y : Math.min(minXY.y, y);
        }

        for (Node n : nodeList) {
            double x = projectXCordMercator(n.longitude) - minXY.x;
            double y = projectYCordMercator(n.latitude) - minXY.y;
            maxXY.x = (maxXY.x == -1) ? x : Math.max(maxXY.x, x);
            maxXY.y = (maxXY.y == -1) ? y : Math.max(maxXY.y, y);
        }
        widthOfBoundingBox = (int) Math.abs(maxXY.x - minXY.x);
        heightOfBoundingBox = (int) Math.abs(maxXY.y - minXY.y);
    }

    private void loadGraph(String fileName) {
        String fileType = fileName.substring(fileName.length() - 3);
        if (fileType.equals("osm")) {
            loadOSM(fileName.substring(0, fileName.length() - 4));
        }
        if (fileType.equals("pbf")) {
            loadPBF(fileName);
        }
        if (gc != null) {
            nodes_label.setText("Number of Nodes: " + graph.getNodeAmount());
            edges_label.setText("Number of Edges: " + graph.getNumberOfEdges());
        }
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
            PBFParser pbfParser = new PBFParser(fileName);
            pbfParser.setDistanceStrategy(distanceStrategy);
            pbfParser.executePBFParser();
            graph = pbfParser.getGraph();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void runAlgorithm() {
        if (selectedNodes.size() <= 1) {
            return;
        }
        ShortestPathResult res = Dijkstra.sssp(graph, selectedNodes.peekFirst().index, selectedNodes.peekLast().index, algorithmMode);
        redrawGraph();
        setLabels(Util.roundDouble(res.d), res.visitedNodes);
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
        double adjustedX1 = getNodeScreenPosX(node);
        double adjustedY1 = getNodeScreenPosY(node);
        double radius = 6;
        double shift = radius / 2;
        gc.fillOval(adjustedX1 - shift, adjustedY1 - shift, radius, radius);
        gc.strokeOval(adjustedX1 - shift, adjustedY1 - shift, radius, radius);
    }

    private void drawAllEdges() {
        List<Node> nodeList = graph.getNodeList();
        List<List<Edge>> adjList = graph.getAdjList();
        // Resets the 'isDrawn' property inside Edges.
        resetIsDrawn(adjList);
        // Iterates through all nodes
        for (int i = 0; i < adjList.size(); i++) {
            Node nx = nodeList.get(i);
            // Iterates through adjacent nodes/edges
            for (Edge edge : adjList.get(i)) {
                Node ny = nodeList.get(edge.to);
                Edge oppositeEdge = findOppositeEdge(adjList, i, edge);
                // If the edge doesn't violate these constrains it will be drawn in the Canvas.
                if (oppositeEdge == null || edge.isBetter(oppositeEdge)) {
                    double adjustedX1 = getNodeScreenPosX(nx);
                    double adjustedY1 = getNodeScreenPosY(nx);

                    double adjustedX2 = getNodeScreenPosX(ny);
                    double adjustedY2 = getNodeScreenPosY(ny);

                    gc.setStroke(chooseEdgeColor(edge));
                    gc.setLineWidth(chooseEdgeWidth(edge));
                    gc.strokeLine(adjustedX1, adjustedY1, adjustedX2, adjustedY2);
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
            return Color.DARKTURQUOISE;
        }
        if (edge.visited) {
            return Color.BLUE;
        }
        return Color.BLACK;
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

    private void resetIsDrawn(List<List<Edge>> adjList) {
        for (List<Edge> edgeList : adjList) {
            for (Edge edge : edgeList) {
                edge.isDrawn = false;
            }
        }
    }

    /**
     * @param cord longitude input
     * @return x coordinate in canvas
     */
    private double projectXCordMercator(double cord) {
        final double RADIUS_MAJOR = 6378137.0;
        return (Math.toRadians(cord) * RADIUS_MAJOR) + xOffset;
    }

    private double getNodeScreenPosX(Node node) {
        double x = projectXCordMercator(node.longitude) - minXY.x;
        return x * globalRatio;
    }

    /**
     * @param cord latitude input
     * @return y coordinate in canvas
     */
    private double projectYCordMercator(double cord) {
        final double RADIUS_MINOR = 6356752.3142;
        return (Math.log(Math.tan(Math.PI / 4 + Math.toRadians(cord) / 2)) * RADIUS_MINOR) + yOffset;
    }

    private double getNodeScreenPosY(Node node) {
        double y = projectYCordMercator(node.latitude) - minXY.y;
        return canvas.getHeight() - y * globalRatio;
    }

    public double nodeToPointDistance(Node node, double x, double y) {
        double nodeX = getNodeScreenPosX(node);
        double nodeY = getNodeScreenPosY(node);
        return Math.sqrt(Math.pow(nodeX - x, 2) + Math.pow(nodeY - y, 2));
    }

    private Node selectClosestNode(double x, double y) {
        List<Node> nodeList = graph.getNodeList();
        Node closestNode = nodeList.get(0);
        double closestDistance = nodeToPointDistance(closestNode, x, y);
        for (Node node : nodeList) {
            double distance = nodeToPointDistance(node, x, y);
            if (distance < closestDistance) {
                closestNode = node;
                closestDistance = distance;
            }
        }
        return closestNode;
    }

    // Here comes all the eventHandle methods that are called when clicked
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

    public void handleZoomInEvent() {
        zoomFactor *= 1.1f;
        setRatios();
        redrawGraph();
    }

    public void handleZoomOutEvent() {
        zoomFactor *= 0.9f;
        setRatios();
        redrawGraph();
    }

    // W A S D navigation TODO: Find out how arrow keys are triggered
    private EventHandler<? super KeyEvent> onKeyPressed() {
        return event -> {
            switch (event.getCode()) {
                case W: case UP:
                    handleNavUpEvent();
                    break;
                case A: case LEFT:
                    handleNavLeftEvent();
                    break;
                case S: case DOWN:
                    handleNavDownEvent();
                    break;
                case D: case RIGHT:
                    handleNavRightEvent();
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
            // TODO: Make completely smooth by doing reverse mercator
            double factor = 50/zoomFactor;
            double dx = event.getX() - clickX;
            double dy = clickY - event.getY();
            xOffset += factor * dx;
            yOffset += factor * dy;
            clickX = event.getX();
            clickY = event.getY();
            dragCounter++;
            redrawGraph();
        };
    }

    private EventHandler<MouseEvent> onMouseClicked() {
        return event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                onLeftClick(event);
            }
            if (event.getButton() == MouseButton.SECONDARY) {
                onRightClick(event);
            }
        };
    }

    private void onLeftClick(MouseEvent event) {
        if (dragCounter > dragLimit) {
            return;
        }
        double x = event.getX();
        double y = event.getY();
        Node node = selectClosestNode(x, y);
        selectedNodes.addLast(node);
        if (selectedNodes.size() > 1) {
            runAlgorithm();
        } else {
            graph.resetPathTrace();
            redrawGraph();
        }
    }

    private void onRightClick(MouseEvent event) {
        selectedNodes = new ArrayDeque<>();
        graph.resetPathTrace();
        redrawGraph();
    }

    public void handleDijkstraEvent() {
        algorithmMode = DIJKSTRA;
        runAlgorithm();
        setAlgorithmNameLabel();
        selectButton(dijkstraButton);
    }

    public void handleBiDijkstraEvent() {
        algorithmMode = BI_DIJKSTRA;
        runAlgorithm();
        setAlgorithmNameLabel();
        selectButton(biDijkstraButton);
    }

    public void handleAStarEvent() {
        algorithmMode = A_STAR;
        runAlgorithm();
        setAlgorithmNameLabel();
        selectButton(aStarButton);
    }

    public void handleBiAStarEvent() {
        algorithmMode = BI_A_STAR;
        runAlgorithm();
        setAlgorithmNameLabel();
        selectButton(biAStarButton);
    }

    private void selectButton(Button algoButton) {
        PseudoClass pseudoClass = PseudoClass.getPseudoClass("selected");
        dijkstraButton.pseudoClassStateChanged(pseudoClass, false);
        biDijkstraButton.pseudoClassStateChanged(pseudoClass, false);
        aStarButton.pseudoClassStateChanged(pseudoClass, false);
        biAStarButton.pseudoClassStateChanged(pseudoClass, false);
        algoButton.pseudoClassStateChanged(pseudoClass, true);
    }

    public void handleLandmarksEvent() {

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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PBF Files", "*.pbf"),
                new FileChooser.ExtensionFilter("OSM Files", "*.osm")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);
        setUpNewGraph(selectedFile.getAbsolutePath());
    }

    // Some utility methods
    private void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setLabels(String distance, int visitedNodes) {
        setAlgorithmNameLabel();
        distance_label.setText("Total Distance: " + distance);
        nodes_visited_label.setText("Nodes Visited: " + visitedNodes);
    }

    private void setAlgorithmNameLabel() {
        algorithm_label.setText("Algorithm: " + algorithmNames.get(algorithmMode));
    }

    private void setSeedLabel() {
        seed_label.setText("Seed: " + Dijkstra.seed);
    }
}
