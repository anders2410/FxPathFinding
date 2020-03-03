package javafx;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiFunction;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.*;
import paths.*;
import pbfparsing.PBFParser;
import xml.*;

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
    private List<Node> selectedNodes = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        distanceStrategy = Util::sphericalDistance;
        canvas.setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            Node node = selectClosestNode(x, y);
            selectedNodes.add(node);
            clearCanvas();
            drawGraph();
        });
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);

        setUpNewGraph("malta-latest.osm.pbf");
    }

    private void setUpNewGraph(String fileName) {
        loadGraph(fileName);
        setGraphBounds();
        zoomFactor = 1;
        setRatios();
        drawGraph();
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
    private void drawGraph() {
        for (Node selectedNode : selectedNodes) {
            double x1 = projectXCordMercator(selectedNode.longitude) - minXY.x;
            double y1 = projectYCordMercator(selectedNode.latitude) - minXY.y;

            double adjustedX1 = ((x1 * globalRatio));
            double adjustedY1 = (canvas.getHeight() - (y1 * globalRatio));
            gc.setFill(Color.PURPLE);
            gc.fillRect(adjustedX1, adjustedY1, 10, 10);

        }
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
                    double x1 = projectXCordMercator(nx.longitude) - minXY.x;
                    double y1 = projectYCordMercator(nx.latitude) - minXY.y;

                    double x2 = projectXCordMercator(ny.longitude) - minXY.x;
                    double y2 = projectYCordMercator(ny.latitude) - minXY.y;

                    double adjustedX1 = ((x1 * globalRatio));
                    double adjustedY1 = (canvas.getHeight() - (y1 * globalRatio));

                    double adjustedX2 = ((x2 * globalRatio));
                    double adjustedY2 = (canvas.getHeight() - (y2 * globalRatio));

                    /*System.out.println("---------------");
                    System.out.println("(" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + ")");
                    System.out.println("(" + adjustedX1 + "," + adjustedY1 + ") -> (" + adjustedX2 + "," + adjustedY2 + ")");*/

                    gc.setStroke(Color.BLACK);
                    if (edge.visited) {
                        gc.setStroke(Color.BLUE);
                    }
                    if (edge.inPath) {
                        gc.setStroke(Color.RED);
                    }
                    gc.strokeLine(adjustedX1, adjustedY1, adjustedX2, adjustedY2);
                    edge.isDrawn = true;
                }
            }
        }
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

    /**
     * @param cord latitude input
     * @return y coordinate in canvas
     */
    private double projectYCordMercator(double cord) {
        final double RADIUS_MINOR = 6356752.3142;
        return (Math.log(Math.tan(Math.PI / 4 + Math.toRadians(cord) / 2)) * RADIUS_MINOR) + yOffset;
    }

    public double nodeToPointDistance(Node node, double x, double y) {
        double nodeX = projectXCordMercator(node.longitude);
        double nodeY = projectYCordMercator(node.latitude);
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
        clearCanvas();
        yOffset -= (zoomFactor <= 1) ? ((0.1 * heightOfBoundingBox * mapHeightRatio) / zoomFactor) :
                ((0.1 * heightOfBoundingBox * mapHeightRatio) / (2.5 * zoomFactor));
        drawGraph();
    }

    public void handleNavDownEvent() {
        clearCanvas();
        yOffset += (zoomFactor <= 1) ? ((0.1 * heightOfBoundingBox * mapHeightRatio) / zoomFactor) :
                ((0.1 * heightOfBoundingBox * mapHeightRatio) / (2.5 * zoomFactor));
        drawGraph();
    }

    public void handleNavLeftEvent() {
        clearCanvas();
        xOffset += (zoomFactor <= 1) ? ((0.1 * widthOfBoundingBox * mapWidthRatio) / zoomFactor) :
                ((0.1 * widthOfBoundingBox * mapWidthRatio) / (2.5 * zoomFactor));
        drawGraph();
    }

    public void handleNavRightEvent() {
        clearCanvas();
        xOffset -= (zoomFactor <= 1) ? ((0.1 * widthOfBoundingBox * mapWidthRatio) / zoomFactor) :
                ((0.1 * widthOfBoundingBox * mapWidthRatio) / (2.5 * zoomFactor));
        drawGraph();
    }

    public void handleZoomInEvent() {
        zoomFactor *= 1.1f;
        setRatios();
        clearCanvas();
        drawGraph();
    }

    public void handleZoomOutEvent() {
        zoomFactor *= 0.9f;
        setRatios();
        clearCanvas();
        drawGraph();
    }

    public void handleDijkstraEvent() {
        clearCanvas();
        Dijkstra.setDistanceStrategy(distanceStrategy);
        ShortestPathResult res = Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA);
        drawGraph();
        setLabels("Dijkstra", Util.roundDouble(res.d), res.visitedNodes);
    }

    public void handleBiDijkstraEvent(ActionEvent actionEvent) {
        clearCanvas();
        Dijkstra.setDistanceStrategy(distanceStrategy);
        ShortestPathResult res = Dijkstra.randomPath(graph, AlgorithmMode.BI_DIJKSTRA);
        drawGraph();
        setLabels("Bidirectional Dijkstra", Util.roundDouble(res.d), res.visitedNodes);
    }

    public void handleAStarEvent() {
        clearCanvas();
        Dijkstra.setDistanceStrategy(distanceStrategy);
        ShortestPathResult res = Dijkstra.randomPath(graph, AlgorithmMode.A_STAR);
        drawGraph();
        setLabels("A*", Util.roundDouble(res.d), res.visitedNodes);
    }

    public void handleBiAStarEvent(ActionEvent actionEvent) {
        clearCanvas();
        Dijkstra.setDistanceStrategy(distanceStrategy);
        ShortestPathResult res = Dijkstra.randomPath(graph, AlgorithmMode.BI_A_STAR);
        drawGraph();
        setLabels("Bidirectional A*", Util.roundDouble(res.d), res.visitedNodes);
    }

    public void handleSeedEvent() {
        Dijkstra.seed++;
    }

    public void handleChooseFileEvent() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PBF Files", "*.pbf"),
                new FileChooser.ExtensionFilter("OSM Files", "*.osm")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);
        clearCanvas();
        setUpNewGraph(selectedFile.getAbsolutePath());
    }

    // Some utility methods
    private void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setLabels(String algo, String distance, int visitedNodes) {
        algorithm_label.setText("Algorithm: " + algo);
        distance_label.setText("Total Distance: " + distance);
        nodes_visited_label.setText("Nodes Visited: " + visitedNodes);
    }
}
