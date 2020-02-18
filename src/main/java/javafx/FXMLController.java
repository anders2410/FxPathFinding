package javafx;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
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
import xml.XMLFilter;
import xml.XMLGraphExtractor;

public class FXMLController implements Initializable {

    public Canvas canvas;

    Graph graph;
    String fileName = "jelling";
    GraphicsContext gc;
    private int coordToPos = 1000;
    private int zoom = 10000000;
    int xOffset;
    int yOffset;
    double canvasHeight;
    double canvasWidth;
    private double mapWidthRatio;
    private double mapHeightRatio;
    private PixelPoint minXY;
    private PixelPoint maxXY;
    private double globalRatio;
    private float zoomFactor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setUp();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setUp() throws FileNotFoundException {
        PBFParser pbfParser = new PBFParser("malta-latest.osm.pbf");
        pbfParser.executePBFParser();
        graph = pbfParser.getGraph();
        // System.out.println(canvasHeight);
        gc = canvas.getGraphicsContext2D();
        canvasHeight = canvas.getHeight();
        canvasWidth = canvas.getWidth();
        zoomFactor = 1;
        minXY = new PixelPoint(-1, -1);
        maxXY = new PixelPoint(-1, -1);

        List<Node> nodeList = graph.getNodeList();
        for (int i = 0; i < nodeList.size(); i++) {
            Node n = nodeList.get(i);
            double x = projectXCordMercator(n.longitude);
            double y = projectYCordMercator(n.latitude);

            minXY.x = (minXY.x == -1) ? x : Math.min(minXY.x, x);
            minXY.y = (minXY.y == -1) ? y : Math.min(minXY.y, y);
        }

        for (int i = 0; i < nodeList.size(); i++) {
            Node n = nodeList.get(i);
            double x = projectXCordMercator(n.longitude) - minXY.x;
            double y = projectYCordMercator(n.latitude) - minXY.y;
            maxXY.x = (maxXY.x == -1) ? x : Math.max(maxXY.x, x);
            maxXY.y = (maxXY.y == -1) ? y : Math.max(maxXY.y, y);
        }
        setRatios();
        gc.setStroke(Color.VIOLET);
        gc.setLineWidth(1.0);
        List<List<Edge>> adjList = graph.getAdjList();
        resetIsDrawn(adjList);
        Gerbil();
    }

    private void setRatios() {
        // determine the width and height ratio because we need to magnify the map to fit into the given image dimension
        mapWidthRatio = zoomFactor * canvasWidth / maxXY.x;
        mapHeightRatio = zoomFactor * canvasHeight / maxXY.y;
        // using different ratios for width and height will cause the map to be stretched. So, we have to determine
        // the global ratio that will perfectly fit into the given image dimension
        globalRatio = Math.min(mapWidthRatio, mapHeightRatio);
    }

    private void Gerbil() {
        List<Node> nodeList = graph.getNodeList();
        List<List<Edge>> adjList = graph.getAdjList();
        resetIsDrawn(adjList);
        for (int i = 0; i < adjList.size(); i++) {
            Node nx = nodeList.get(i);
            for (Edge edge : adjList.get(i)) {
                Node ny = nodeList.get(edge.to);
                Edge oppositeEdge = findOppositeEdge(adjList, i, edge);
                if (oppositeEdge == null || edge.isBetter(oppositeEdge)) {
                    double x1 = projectXCordMercator(nx.longitude) - minXY.x;
                    double y1 = projectYCordMercator(nx.latitude) - minXY.y;

                    double x2 = projectXCordMercator(ny.longitude) - minXY.x;
                    double y2 = projectYCordMercator(ny.latitude) - minXY.y;

                    double adjustedX1 = ((x1 * globalRatio));
                    double adjustedY1 = (canvasHeight - (y1 * globalRatio));

                    double adjustedX2 = ((x2 * globalRatio));
                    double adjustedY2 = (canvasHeight - (y2 * globalRatio));
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

    private double projectXCordMercator(double cord) {
        final double RADIUS_MAJOR = 6378137.0;
        return (Math.toRadians(cord) * RADIUS_MAJOR) + xOffset;
    }

    private double projectYCordMercator(double cord) {
        final double RADIUS_MINOR = 6356752.3142;
        return (Math.log(Math.tan(Math.PI / 4 + Math.toRadians(cord) / 2)) * RADIUS_MINOR) + yOffset;
    }

    // Here comes all the eventHandle methods
    public void handleNavUpEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        yOffset -= 1000;
        Gerbil();
    }

    public void handleNavDownEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        yOffset += 1000;
        Gerbil();
    }

    public void handleNavLeftEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        xOffset += 1000;
        Gerbil();
    }

    public void handleNavRightEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        xOffset -= 1000;
        Gerbil();
    }

    public void handleZoomInEvent() {
        zoomFactor *= 1.1f;
        setRatios();
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        Gerbil();
    }

    public void handleZoomOutEvent() {
        zoomFactor *= 0.9f;
        setRatios();
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        Gerbil();
    }

    public void handleDjikstraEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA);
        Gerbil();
    }

    public void handleAStarEvent() {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        Dijkstra.randomPath(graph, AlgorithmMode.A_STAR_DIST);
        Gerbil();
    }

    public void handleCanvasDrawing() {
        Gerbil();
    }

    public void handleSeedEvent() {
        Dijkstra.seed++;
    }
}
