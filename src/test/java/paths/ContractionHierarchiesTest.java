package paths;

import javafx.util.Pair;
import load.GraphIO;
import load.pbfparsing.PBFParser;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

public class ContractionHierarchiesTest {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";
    private GraphIO graphIO;

    @Before
    public void setUp() {
        PBFParser pbfParser = new PBFParser(fileName);
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        BiFunction<Node, Node, Double> distanceStrategy2 = Util::sphericalDistance;

        SSSP.setDistanceStrategy(distanceStrategy1);
        /*pbfParser.setDistanceStrategy(distanceStrategy2);
        try {
            pbfParser.executePBFParser();
        } catch (IOException e) {
            e.printStackTrace();
        }
        graph = pbfParser.getGraph();
        GraphUtil g = new GraphUtil(graph);
        graph = g.scc().get(0);
        SSSP.setGraph(graph);*/
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        System.out.println("------------------------------ ORIGINAL GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + graph.getNodeAmount());
        System.out.println("Number of edges: " + graph.getEdgeAmount());
    }

    @Test
    public void testContractionHierarchiesMainFunction() {
        ShortestPathResult normalDijkRes = SSSP.findShortestPath(24, 256, AlgorithmMode.DIJKSTRA);

        ContractionHierarchies contractionHierarchies = new ContractionHierarchies(graph);
        Pair<Graph, List<Integer>> pair = contractionHierarchies.preprocess();
        Graph CHGraph = pair.getKey();
        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + CHGraph.getNodeAmount());
        System.out.println("Number of edges: " + CHGraph.getEdgeAmount());


        double distance = contractionHierarchies.computeDist(CHGraph,24, 256);
        SSSP.setGraph(graph);
        normalDijkRes = SSSP.findShortestPath(24, 256, AlgorithmMode.DIJKSTRA);
        System.out.println(normalDijkRes.path);
        System.out.println(normalDijkRes.d);
        System.out.println("Overall distance: " + distance );
        double distance1 = contractionHierarchies.computeDist(CHGraph,25, 256);
        System.out.println("Overall distance: " + distance1);
        normalDijkRes = SSSP.findShortestPath(25, 256, AlgorithmMode.DIJKSTRA);
        System.out.println(normalDijkRes.path);
        System.out.println(normalDijkRes.d);
        double distance2 = contractionHierarchies.computeDist(CHGraph,26, 256);
        normalDijkRes = SSSP.findShortestPath(26, 256, AlgorithmMode.DIJKSTRA);
        System.out.println(normalDijkRes.path);
        System.out.println(normalDijkRes.d);
        System.out.println("Overall distance: " + distance2);
        double distance3 = contractionHierarchies.computeDist(CHGraph,1025, 8627);
        normalDijkRes = SSSP.findShortestPath(1025, 8627, AlgorithmMode.DIJKSTRA);
        System.out.println(normalDijkRes.path);
        System.out.println(normalDijkRes.d);
        System.out.println("Overall distance: " + distance3);
    }
}
