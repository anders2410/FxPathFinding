package paths;

import javafx.util.Pair;
import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

public class ContractionHierarchiesTest {
    Graph originalGraph;
    String fileName = "malta-latest.osm.pbf";
    private GraphIO graphIO;

    @Before
    public void setUp() {
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        originalGraph = graphIO.getGraph();
        SSSP.setGraph(originalGraph);

        System.out.println("------------------------------ ORIGINAL GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + originalGraph.getNodeAmount());
        System.out.println("Number of edges: " + originalGraph.getEdgeAmount());
    }

    @Test
    public void testContractionHierarchiesNotIntegrated() {
        ContractionHierarchies contractionHierarchies = new ContractionHierarchies(originalGraph);
        ContractionHierarchiesResult contractionHierarchiesResult = contractionHierarchies.preprocess();
        SSSP.setContractionHierarchiesResult(contractionHierarchiesResult);

        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + contractionHierarchiesResult.getGraph().getNodeAmount());
        System.out.println("Number of edges: " + contractionHierarchiesResult.getGraph().getEdgeAmount());


        System.out.println("------------------------------ NOT-INTEGRATED VS. DIJKSTRA ----------------------------------------");
        ShortestPathResult dijkstraResult;

        int seed = 0;
        for (int i = 0; i < 40; i++) {
            Random random = new Random(seed);
            int source = random.nextInt(originalGraph.getNodeAmount());
            int target = random.nextInt(originalGraph.getNodeAmount());

            SSSP.setGraph(originalGraph);
            dijkstraResult = SSSP.findShortestPath(source, target, AlgorithmMode.DIJKSTRA);

            Pair<Double, Set<Integer>> CHResult = contractionHierarchies.computeDist(contractionHierarchiesResult.getGraph(), source, target);

            System.out.println(dijkstraResult.path);
            System.out.println(CHResult.getValue());
            System.out.println("Dijkstra distance: " + dijkstraResult.d);
            System.out.println("CH distance: " + CHResult.getKey());

            seed++;
        }
    }

    @Test
    public void testContractionHierarchiesIntegrated() {
        ContractionHierarchies contractionHierarchies = new ContractionHierarchies(originalGraph);
        ContractionHierarchiesResult contractionHierarchiesResult = contractionHierarchies.preprocess();
        SSSP.setContractionHierarchiesResult(contractionHierarchiesResult);

        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + contractionHierarchiesResult.getGraph().getNodeAmount());
        System.out.println("Number of edges: " + contractionHierarchiesResult.getGraph().getEdgeAmount());


        System.out.println("------------------------------ TESTING VS. DIJKSTRA -----------------------------------------------");
        ShortestPathResult dijkstraResult;
        ShortestPathResult CHResult;

        int seed = 0;
        for (int i = 0; i < 10; i++) {
            Random random = new Random(seed);
            int source = random.nextInt(originalGraph.getNodeAmount());
            int target = random.nextInt(originalGraph.getNodeAmount());

            SSSP.setGraph(originalGraph);
            System.out.println(SSSP.getGraph());
            dijkstraResult = SSSP.findShortestPath(source, target, AlgorithmMode.DIJKSTRA);
            SSSP.setGraph(contractionHierarchiesResult.getGraph());
            System.out.println(SSSP.getGraph());
            CHResult = SSSP.findShortestPath(source, target, AlgorithmMode.CONTRACTION_HIERARCHIES);
            System.out.println(contractionHierarchiesResult.getGraph());
            Pair<Double, Set<Integer>> CHTest = contractionHierarchies.computeDist(contractionHierarchiesResult.getGraph(), source, target);

            System.out.println(dijkstraResult.path);
            System.out.println(CHResult.path);
            System.out.println(CHTest.getValue());
            System.out.println("Dijkstra distance: " + dijkstraResult.d);
            System.out.println("CH distance: " + CHResult.d);
            System.out.println("CH test distance: " + CHTest.getKey());

            seed++;
        }
    }

    @Test
    public void testContractionHierarchiesSinglePath() {
        ContractionHierarchies contractionHierarchies = new ContractionHierarchies(originalGraph);
        ContractionHierarchiesResult contractionHierarchiesResult = contractionHierarchies.preprocess();
        SSSP.setContractionHierarchiesResult(contractionHierarchiesResult);

        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + contractionHierarchiesResult.getGraph().getNodeAmount());
        System.out.println("Number of edges: " + contractionHierarchiesResult.getGraph().getEdgeAmount());


        System.out.println("------------------------------ NOT-INTEGRATED VS. DIJKSTRA ----------------------------------------");
        ShortestPathResult dijkstraResult;

        int source = 5640;
        int target = 4779;

        SSSP.setGraph(originalGraph);
        dijkstraResult = SSSP.findShortestPath(source, target, AlgorithmMode.DIJKSTRA);

        Pair<Double, Set<Integer>> CHResult = contractionHierarchies.computeDist(contractionHierarchiesResult.getGraph(), source, target);

        System.out.println(dijkstraResult.path);
        System.out.println(CHResult.getValue());
        System.out.println("Dijkstra distance: " + dijkstraResult.d);
        System.out.println("CH distance: " + CHResult.getKey());
    }
}
