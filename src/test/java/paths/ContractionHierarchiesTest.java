package paths;

import javafx.util.Pair;
import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.BiFunction;

import static org.junit.Assert.fail;
import static paths.SSSP.seed;

public class ContractionHierarchiesTest {
    Graph originalGraph;
    String fileName = "malta-latest.osm.pbf";
    GraphIO graphIO;
    ContractionHierarchies contractionHierarchies;
    ContractionHierarchiesResult contractionHierarchiesResult;

    @Before
    public void setUp() {
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        originalGraph = graphIO.getGraph();
        SSSP.setGraph(originalGraph);

        contractionHierarchies = new ContractionHierarchies(originalGraph);
        contractionHierarchiesResult = contractionHierarchies.preprocess();
        SSSP.setContractionHierarchiesResult(contractionHierarchiesResult);

        System.out.println("------------------------------ ORIGINAL GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + originalGraph.getNodeAmount());
        System.out.println("Number of edges: " + originalGraph.getEdgeAmount());
    }

    @Test
    public void testContractionHierarchiesNotIntegrated() {
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
    public void testContractionHierarchiesSinglePathNotIntegrated() {
        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + contractionHierarchiesResult.getGraph().getNodeAmount());
        System.out.println("Number of edges: " + contractionHierarchiesResult.getGraph().getEdgeAmount());


        System.out.println("------------------------------ NOT-INTEGRATED VS. DIJKSTRA ----------------------------------------");
        ShortestPathResult dijkstraResult;

        int source = 5339;
        int target = 5147;

        SSSP.setGraph(originalGraph);
        dijkstraResult = SSSP.findShortestPath(source, target, AlgorithmMode.DIJKSTRA);
        Pair<Double, Set<Integer>> CHResult = contractionHierarchies.computeDist(contractionHierarchiesResult.getGraph(), source, target);

        System.out.println(dijkstraResult.path);
        System.out.println(CHResult.getValue());
        System.out.println("Dijkstra distance: " + dijkstraResult.d);
        System.out.println("CH distance: " + CHResult.getKey());
    }

    @Test
    public void testContractionHierarchiesSinglePathIntegrated() {
        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + contractionHierarchiesResult.getGraph().getNodeAmount());
        System.out.println("Number of edges: " + contractionHierarchiesResult.getGraph().getEdgeAmount());


        System.out.println("--------------------------- SINGLE INTEGRATED VS. DIJKSTRA -------------------------------");
        ShortestPathResult dijkstraResult;
        ShortestPathResult CHResult;

        int source = 10605;
        int target = 5340;

        System.out.println("Original Graph");
        originalGraph.getAdjList().get(5339).forEach(System.out::println);
        System.out.println("Augmented Graph");
        contractionHierarchiesResult.getGraph().getAdjList().get(5339).forEach(System.out::println);

        SSSP.setGraph(originalGraph);
        dijkstraResult = SSSP.findShortestPath(source, target, AlgorithmMode.DIJKSTRA);

        System.out.println(SSSP.getGraph());
        CHResult = SSSP.findShortestPath(source, target, AlgorithmMode.CONTRACTION_HIERARCHIES);

        // CHResult.path.forEach(i -> contractionHierarchiesResult.getGraph().getAdjList().get(i).forEach(System.out::println));

        System.out.println(dijkstraResult.path);
        System.out.println(CHResult.path);
        System.out.println("Dijkstra distance: " + dijkstraResult.d);
        System.out.println("CH distance: " + CHResult.d);
    }

    int[] matrix;
    int testCases, i;
    double[][] runtimes;
    List<TestResult> failList;

    @Test
    public void testContractionHierarchiesVsDijkstraMultiple() {
        int algorithms = 1;
        matrix = new int[algorithms];

        testCases = 100;
        runtimes = new double[algorithms][testCases];
        i = 0;
        failList = new ArrayList<>();
        seed = 0;

        while (i < testCases) {
            seed++;
            SSSP.setGraph(originalGraph);
            ShortestPathResult dijkstraResult = SSSP.randomPath(AlgorithmMode.DIJKSTRA);
            if (dijkstraResult.path.size() > 20) {
                continue;
            }
            runtimes[0][i] = dijkstraResult.runTime;
            double dijkstraDist = dijkstraResult.d;
            List<Integer> dijkstraPath = dijkstraResult.path;

            // Running the test algorithm
            SSSP.setGraph(contractionHierarchiesResult.getGraph());
            testSingle(dijkstraDist, dijkstraPath, AlgorithmMode.CONTRACTION_HIERARCHIES, 0);
            i++;
        }

        System.out.println(matrix[0]);
        if (Arrays.equals(new int[algorithms], matrix)) {
            System.out.println(Arrays.deepToString(runtimes));
        } else fail();
    }

    private void testSingle(double dijkstraDist, List<Integer> dijkstraPath, AlgorithmMode algoMode, int algoNumber) {
        ShortestPathResult algoResult = SSSP.randomPath(algoMode);
        runtimes[algoNumber][i] = algoResult.runTime;
        double algoDist = algoResult.d;
        List<Integer> algoPath = algoResult.path;
        if (!algoPath.equals(dijkstraPath)) {
            matrix[algoNumber]++;
            failList.add(new TestResult(dijkstraPath.get(0), dijkstraPath.get(dijkstraPath.size() - 1), dijkstraPath, algoPath, dijkstraDist, algoDist));
        }
    }
}

class TestResult {
    int source;
    int target;
    List<Integer> dijkstraPath;
    List<Integer> algoPath;
    double dijkstraDist;
    double algoDist;

    public TestResult(int source, int target, List<Integer> dijkstraPath, List<Integer> algoPath,
                      double dijkstraDist, double algoDist) {
        this.source = source;
        this.target = target;
        this.dijkstraPath = dijkstraPath;
        this.algoPath = algoPath;
        this.dijkstraDist = dijkstraDist;
        this.algoDist = algoDist;
    }
}
