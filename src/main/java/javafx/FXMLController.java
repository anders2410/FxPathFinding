package javafx;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.Edge;
import model.Graph;
import model.Node;
import paths.AlgorithmMode;
import paths.Dijkstra;
import pbfparsing.PBFParser;

public class FXMLController implements Initializable {

    public Canvas canvas;

    Graph graph;
    String fileName = "jelling";
    GraphicsContext gc;
    private int coordToPos = 1000;
    private int zoom = 10000000;
    int xOffset = -3800;
    int yOffset = 7700;
    double canvasHeight;
    double canvasWidth;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setUp();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        handleCanvasDrawing();
    }

    public void setUp() throws FileNotFoundException {
        /*XMLFilter xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.executeExtractor();
        graph = xmlGraphExtractor.getGraph();*/
        PBFParser pbfParser = new PBFParser("isle-of-man-latest.osm.pbf");
        pbfParser.executePBFParser();
        graph = pbfParser.getGraph();
        // System.out.println(graph.getAdjList());
        gc = canvas.getGraphicsContext2D();
        canvasHeight = canvas.getHeight();
        canvasWidth = canvas.getWidth();
    }

    private void drawEdges() {
        gc.setStroke(Color.VIOLET);
        gc.setLineWidth(1.0);
        List<Node> nodeList = graph.getNodeList();
        List<List<Edge>> adjList = graph.getAdjList();
        resetIsDrawn(adjList);
        for (int i = 0; i < adjList.size(); i++) {
            Node nx = nodeList.get(i);
            for (Edge edge : adjList.get(i)) {
                Node ny = nodeList.get(edge.to);
                Edge oppositeEdge = findOppositeEdge(adjList, i, edge);
                if (oppositeEdge == null || edge.isBetter(oppositeEdge)) {
                    drawEdge(nx, ny, edge);
                }
            }
        }
    }

    private Edge findOppositeEdge(java.util.List<java.util.List<Edge>> adjList, int i, Edge edge) {
        Edge oppositeEdge = null;
        for (Edge edgeTo : adjList.get(edge.to)) {
            if (edgeTo.to == i) {
                oppositeEdge = edgeTo;
            }
        }
        return oppositeEdge;
    }

    private void resetIsDrawn(java.util.List<java.util.List<Edge>> adjList) {
        for (List<Edge> edgeList : adjList) {
            for (Edge edge : edgeList) {
                edge.isDrawn = false;
            }
        }
    }

    private void drawEdge(Node nx, Node ny, Edge edge) {
        /*float x1 = projectCord(nx.latitude, xOffset);
        float y1 = projectCord(-nx.longitude, yOffset);
        float x2 = projectCord(ny.latitude, xOffset);
        float y2 = projectCord(-ny.longitude, yOffset);*/

        double x1 = projectXCordMercator(nx.latitude);
        double y1 = projectYCordMercator(nx.longitude);
        double x2 = projectXCordMercator(ny.latitude);
        double y2 = projectYCordMercator(ny.longitude);

        System.out.println("(" + x1 + ", " + y1 + ") -> (" + x2 + ", " + y2 + ")");
        gc.setStroke(Color.BLACK);
        if (edge.visited) {
            gc.setStroke(Color.BLUE);
        }
        if (edge.inPath) {
            gc.setStroke(Color.RED);
        }
        gc.strokeLine(x1, y1, x2, y2);
        edge.isDrawn = true;
        //graphicsContext.drawString("" + Math.round(edge.d), (x1 + x2) * 0.5f, (y1 + y2) * 0.5f);
    }

    private double projectXCordMercator(double cord) {
        final double RADIUS_MAJOR = 6378137.0;
        return Math.toRadians(cord) * RADIUS_MAJOR;
    }

    private double projectYCordMercator(double cord) {
        final double RADIUS_MINOR = 6356752.3142;
        return Math.log(Math.tan(Math.PI / 4 + Math.toRadians(cord) / 2)) * RADIUS_MINOR;
    }

    private float projectCord(float cord, int shift) {
        return (cord % zoom) / coordToPos + shift;
    }

    // Here comes all the eventHandle methods
    public void handleNavUpEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        yOffset += 100;
        drawEdges();
    }

    public void handleNavDownEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        yOffset -= 100;
        drawEdges();
    }

    public void handleNavLeftEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        xOffset += 100;
        drawEdges();
    }

    public void handleNavRightEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        xOffset -= 100;
        drawEdges();
    }

    public void handleZoomInEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        canvasWidth *= 0.67;
        canvasHeight *= 0.67;
        gc.scale(1.5, 1.5);
        drawEdges();
    }

    public void handleZoomOutEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        canvasWidth *= 1.5;
        canvasHeight *= 1.5;
        gc.scale(0.67, 0.67);
        drawEdges();
    }

    public void handleDjikstraEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA);
        drawEdges();
    }

    public void handleAStarEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        Dijkstra.randomPath(graph, AlgorithmMode.A_STAR_DIST);
        drawEdges();
    }

    public void handleCanvasDrawing() {
        drawEdges();
    }
}
