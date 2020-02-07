package javafx;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import model.Edge;
import model.Graph;
import model.Node;
import xml.XMLFilter;
import xml.XMLGraphExtractor;

public class FXMLController implements Initializable {

    public Canvas canvas;
    public Button button1;

    Graph graph;
    String fileName = "jelling";
    private int coordToPos = 1000;
    private int zoom = 10000000;
    int xOffset = -3800;
    int yOffset = 7700;

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
    }

    public void handleButton1Event() {
        System.out.println("The bytton1 has been pressed");
        yOffset -= 100;
    }

    public void handleCanvasDrawing() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawEdges(gc);
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
}
