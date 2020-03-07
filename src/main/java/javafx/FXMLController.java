package javafx;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
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
import java.util.List;
import java.util.function.BiFunction;

import static model.Util.algorithmNames;
import static paths.AlgorithmMode.*;
import static paths.Dijkstra.*;

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
    private PixelPoint minXY = new PixelPoint(-1, -1);
    private PixelPoint maxXY = new PixelPoint(-1, -1);
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
        canvas.setOnScroll(onMouseScrolled());
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);
        setUpNewGraph("denmark-latest.osm.pbf");
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
        graph.resetPathTrace();
        Deque<Node> selectedNodesCopy = new ArrayDeque<>(selectedNodes);
        selectedNodesCopy.pollFirst();
        Node lastNode = selectedNodes.pollLast();
        assert selectedNodes.size() == selectedNodesCopy.size();
        List<ShortestPathResult> results = new ArrayList<>();
        for (Node fromNode : selectedNodes) {
            Node toNode = selectedNodesCopy.pollFirst();
            assert toNode != null;
            results.add(Dijkstra.sssp(graph, fromNode.index, toNode.index, algorithmMode));
        }
        selectedNodes.addLast(lastNode);
        redrawGraph();
        Optional<ShortestPathResult> optCombinedRes = results.stream().reduce((res1, res2) -> {
                    List<Integer> combinedPath = new ArrayList<>(res1.path);
                    combinedPath.addAll(res2.path.subList(1, res2.path.size()));
                    return new ShortestPathResult(res1.d + res2.d, combinedPath, res1.visitedNodes + res2.visitedNodes);
                });
        if (optCombinedRes.isPresent()) {
            ShortestPathResult combinedRes = optCombinedRes.get();
            setLabels(Util.roundDouble(combinedRes.d), combinedRes.visitedNodes);
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
        double scaleFactor = Math.min(totalrounds % ((double) roundVisit / (double) totalrounds), 1);
        Color newcolor = color.deriveColor(1, 1, scaleFactor, 1);
        return color;
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

    //Projections

    PixelPoint toScreenPos(Node node) {
        return new PixelPoint(toScreenPosX(node.longitude), toScreenPosY(node.latitude));
    }

    Node toNode(PixelPoint p) {
        return new Node(-1, toLatitude(p.y), toLongitude(p.x));
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
        return Math.toDegrees((x - xOffset)/RADIUS_MAJOR);
    }

    double toScreenPosX(double longitude) {
        double x = mercatorX(longitude) - minXY.x;
        return x * globalRatio;
    }

    double toLongitude(double x) {
        return invMercatorX(x/globalRatio + minXY.x);
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
        return Math.toDegrees(2*Math.atan(Math.exp((y - yOffset)/RADIUS_MINOR)) - Math.PI/2);
    }

    double toScreenPosY(double latitude) {
        double y = mercatorY(latitude) - minXY.y;
        return canvas.getHeight() - y * globalRatio;
    }

    double toLatitude(double y) {
        return invMercatorY((canvas.getHeight() - y)/globalRatio + minXY.y);
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
        return new PixelPoint(canvas.getWidth()/2,canvas.getHeight()/2);
    }

    private void shiftOffset(PixelPoint point, PixelPoint newLocation) {
        xOffset += newLocation.x - point.x;
        yOffset += newLocation.y - point.y;
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
        Node centerNode = toNode(getScreenCenter());
        System.out.println("Center point before zoom " + getScreenCenter());
        System.out.println(centerNode);
        System.out.println(selectClosestNode(getScreenCenter()));
        System.out.println("Center nodepoint after zoom " + toScreenPos(centerNode));

        zoomFactor *= 1.1f;
        setRatios();

        //shiftOffset(toScreenPos(centerNode), getScreenCenter());
        redrawGraph();
    }

    public void handleZoomOutEvent() {
        zoomFactor *= 1/1.1f;
        setRatios();
        redrawGraph();
    }

    // W A S D navigation TODO: Find out how arrow keys are triggered
    private EventHandler<? super KeyEvent> onKeyPressed() {
        return event -> {
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
            double factor = 50 / zoomFactor;
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
                onRightClick();
            }
        };
    }

    private void onLeftClick(MouseEvent event) {
        if (dragCounter > dragLimit) {
            return;
        }
        PixelPoint mousePos = new PixelPoint(event.getX(), event.getY());
        Node node = selectClosestNode(mousePos);
        selectedNodes.addLast(node);
        if (selectedNodes.size() > 1) {
            runAlgorithm();
        } else {
            graph.resetPathTrace();
            redrawGraph();
        }
    }

    private void onRightClick() {
        selectedNodes = new ArrayDeque<>();
        graph.resetPathTrace();
        redrawGraph();
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
        selectButton(dijkstraButton);
    }

    public void handleBiDijkstraEvent() {
        algorithmMode = BI_DIJKSTRA;
        runAlgorithm();
        setAlgorithmLabels();
        selectButton(biDijkstraButton);
    }

    public void handleAStarEvent() {
        algorithmMode = A_STAR;
        runAlgorithm();
        setAlgorithmLabels();
        selectButton(aStarButton);
    }

    public void handleBiAStarEvent() {
        algorithmMode = BI_A_STAR_CONSISTENT;
        runAlgorithm();
        setAlgorithmLabels();
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
        // TODO: Add algorithm for landmarks
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
        source_label.setText("Source: " + Dijkstra.getSource());
        target_label.setText("Target: " + Dijkstra.getTarget());
        setSeedLabel();
    }

    private void setSeedLabel() {
        seed_label.setText("Seed: " + Dijkstra.seed);
    }

    // https://code.makery.ch/blog/javafx-dialogs-official/
    public void handleSetParameterEvent() {
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

        from.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(Integer.parseInt(newValue) > graph.getNodeList().size());
        });

        to.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(Integer.parseInt(newValue) > graph.getNodeList().size());
        });

        gridPane.add(new Label("From:"), 0, 0);
        gridPane.add(from, 1, 0);
        gridPane.add(new Label("To:"), 2, 0);
        gridPane.add(to, 3, 0);
        gridPane.add(new Label("Seed:"), 0, 1);
        gridPane.add(seed, 1, 1);

        dialog.getDialogPane().setContent(gridPane);

        // Request focus on the username field by default.
        Platform.runLater(from::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == acceptButton) {
                onRightClick();
                if (seed.getText().equals("")) {
                    Dijkstra.seed = 0;
                } else {
                    Dijkstra.seed = Integer.parseInt(seed.getText());
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
}
