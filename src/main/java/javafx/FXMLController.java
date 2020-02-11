package javafx;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import model.Edge;
import model.Graph;
import model.Node;
import paths.AlgorithmMode;
import paths.Dijkstra;
import xml.XMLFilter;
import xml.XMLGraphExtractor;

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
        setUp();
        handleCanvasDrawing();
    }

    public void setUp() {
        XMLFilter xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.executeExtractor();
        graph = xmlGraphExtractor.getGraph();
        gc = canvas.getGraphicsContext2D();
        canvasHeight = canvas.getHeight();
        canvasWidth = canvas.getWidth();
    }

    private void drawEdges(GraphicsContext graphicsContext) {
        graphicsContext.setStroke(Color.VIOLET);
        graphicsContext.setLineWidth(1.0);
        java.util.List<Node> nodeList = graph.getNodeList();
        java.util.List<java.util.List<Edge>> adjList = graph.getAdjList();
        resetIsDrawn(adjList);
        for (int i = 0; i < adjList.size(); i++) {
            Node nx = nodeList.get(i);
            for (Edge edge : adjList.get(i)) {
                Node ny = nodeList.get(edge.to);
                Edge oppositeEdge = findOppositeEdge(adjList, i, edge);
                if (oppositeEdge == null || edge.isBetter(oppositeEdge)) {
                    drawEdge(graphicsContext, nx, ny, edge);
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

    private void drawEdge(GraphicsContext graphicsContext, Node nx, Node ny, Edge edge) {
        float x1 = projectCord(nx.latitude, xOffset);
        float y1 = projectCord(-nx.longitude, yOffset);
        float x2 = projectCord(ny.latitude, xOffset);
        float y2 = projectCord(-ny.longitude, yOffset);
        //System.out.println("(" + x1 + ", " + y1 + ") -> (" + x2 + ", " + y2 + ")");
        graphicsContext.setStroke(Color.BLACK);
        if (edge.visited) {
            graphicsContext.setStroke(Color.BLUE);
        }
        if (edge.inPath) {
            graphicsContext.setStroke(Color.RED);
        }
        graphicsContext.strokeLine(x1, y1, x2, y2);
        edge.isDrawn = true;
        //graphicsContext.drawString("" + Math.round(edge.d), (x1 + x2) * 0.5f, (y1 + y2) * 0.5f);
    }

    private float projectCord(float cord, int shift) {
        return (cord % zoom) / coordToPos + shift;
    }

    // Here comes all the eventHandle methods
    public void handleNavUpEvent() {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        yOffset += 100;
        drawEdges(gc);
    }

    public void handleNavDownEvent() {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        yOffset -= 100;
        drawEdges(gc);
    }

    public void handleNavLeftEvent() {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        xOffset += 100;
        drawEdges(gc);
    }

    public void handleNavRightEvent() {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        xOffset -= 100;
        drawEdges(gc);
    }

    public void handleZoomInEvent(ActionEvent actionEvent) {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        canvasWidth *= 0.67;
        canvasHeight *= 0.67;
        gc.scale(1.5,1.5);
        // Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA);
        drawEdges(gc);
    }

    public void handleZoomOutEvent(ActionEvent actionEvent) {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        canvasWidth *= 1.5;
        canvasHeight *= 1.5;
        gc.scale(0.67,0.67);
        // Dijkstra.randomPath(graph, AlgorithmMode.A_STAR_DIST);
        drawEdges(gc);
    }

    public void handleDjikEvent(ActionEvent actionEvent) {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA);
        drawEdges(gc);
    }

    public void handleAStarEvent(ActionEvent actionEvent) {
        gc.clearRect(0,0, canvasWidth, canvasHeight);
        Dijkstra.randomPath(graph, AlgorithmMode.A_STAR_DIST);
        drawEdges(gc);
    }

    public void handleCanvasDrawing() {
        drawEdges(gc);
    }
}
