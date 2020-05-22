package paths;

import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;
import paths.preprocessing.ContractionHierarchies;
import paths.preprocessing.CHResult;

import java.util.*;
import java.util.function.BiFunction;

import static org.junit.Assert.fail;
import static paths.SSSP.seed;

public class ContractionHierarchiesTest {
    Graph originalGraph;
    String fileName = "malta-latest.osm.pbf";
    GraphIO graphIO;
    ContractionHierarchies contractionHierarchies;
    CHResult CHResult;

    @Before
    public void setUp() {
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1, true);
        graphIO.loadGraph(fileName);
        originalGraph = graphIO.getGraph();
        SSSP.setGraph(originalGraph);

        graphIO.loadPreCH(fileName);
        /*contractionHierarchies = new ContractionHierarchies(originalGraph);
        CHResult = contractionHierarchies.preprocess();
        SSSP.setCHResult(CHResult);*/

        System.out.println("------------------------------ ORIGINAL GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + originalGraph.getNodeAmount());
        System.out.println("Number of edges: " + originalGraph.getEdgeAmount());
    }

    @Test
    public void testContractionHierarchiesIntegrated() {
        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + SSSP.getCHResult().getGraph().getNodeAmount());
        System.out.println("Number of edges: " + SSSP.getCHResult().getGraph().getEdgeAmount());


        System.out.println("------------------------------ TESTING VS. DIJKSTRA -----------------------------------------------");
        ShortestPathResult dijkstraResult;
        ShortestPathResult CHResult;

        int seed = 0;
        for (int i = 0; i < 10; i++) {
            Random random = new Random(seed);
            int source = random.nextInt(originalGraph.getNodeAmount());
            int target = random.nextInt(originalGraph.getNodeAmount());

            dijkstraResult = SSSP.findShortestPath(source, target, AlgorithmMode.DIJKSTRA);
            CHResult = SSSP.findShortestPath(source, target, AlgorithmMode.CONTRACTION_HIERARCHIES);

            System.out.println(dijkstraResult.path);
            System.out.println(CHResult.path);
            System.out.println("Dijkstra distance: " + dijkstraResult.d);
            System.out.println("CH distance: " + CHResult.d);
            System.out.println();

            seed++;
        }
    }

    @Test
    public void testContractionHierarchiesSinglePathIntegrated() {
        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + SSSP.getCHResult().getGraph().getNodeAmount());
        System.out.println("Number of edges: " + SSSP.getCHResult().getGraph().getEdgeAmount());


        System.out.println("--------------------------- SINGLE INTEGRATED VS. DIJKSTRA -------------------------------");
        ShortestPathResult dijkstraResult;
        ShortestPathResult CHResult;

        // Paths we had problems with earlier
        /*int source = 7726, 3059, 4408, 5087, 14830;
        int target = 5703, 12090, 14897, 8548, 14830;*/

        int source = 18551;
        int target = 10114;

        dijkstraResult = SSSP.findShortestPath(source, target, AlgorithmMode.DIJKSTRA);
        CHResult = SSSP.findShortestPath(source, target, AlgorithmMode.CONTRACTION_HIERARCHIES);

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

        testCases = 10000;
        runtimes = new double[algorithms][testCases];
        i = 0;
        failList = new ArrayList<>();
        seed = 0;

        while (i < testCases) {
            seed++;
            SSSP.setGraph(originalGraph);
            ShortestPathResult dijkstraResult = SSSP.randomPath(AlgorithmMode.DIJKSTRA);
            runtimes[0][i] = dijkstraResult.runTime;
            double dijkstraDist = dijkstraResult.d;
            List<Integer> dijkstraPath = dijkstraResult.path;

            // Running the test algorithm
            testSingle(dijkstraDist, dijkstraPath);
            i++;
        }

        failList.forEach(t -> System.out.println(t.dijkstraPath + "\n" + t.algorithmPath + "\n"));
        if (Arrays.equals(new int[algorithms], matrix)) {
            System.out.println(Arrays.deepToString(runtimes));
        } else fail();
    }

    private void testSingle(double dijkstraDist, List<Integer> dijkstraPath) {
        ShortestPathResult algorithmResult = SSSP.randomPath(AlgorithmMode.CONTRACTION_HIERARCHIES);
        runtimes[0][i] = algorithmResult.runTime;
        double algorithmDistance = algorithmResult.d;
        List<Integer> algorithmPath = algorithmResult.path;
        if (!compareTwoDoublesWithTolerance(dijkstraDist, algorithmDistance) || !dijkstraPath.equals(algorithmPath)) {
            matrix[0]++;
            failList.add(new TestResult(dijkstraPath.get(0), dijkstraPath.get(dijkstraPath.size() - 1), dijkstraPath, algorithmPath, dijkstraDist, algorithmDistance));
        }
    }

    private boolean compareTwoDoublesWithTolerance(double a, double b) {
        return Math.abs(a - b) <= 0.00000000001 || Double.valueOf(a).equals(b);
    }
}

class TestResult {
    int source;
    int target;
    List<Integer> dijkstraPath;
    List<Integer> algorithmPath;
    double dijkstraDist;
    double algorithmDist;

    public TestResult(int source, int target, List<Integer> dijkstraPath, List<Integer> algorithmPath,
                      double dijkstraDist, double algorithmDist) {
        this.source = source;
        this.target = target;
        this.dijkstraPath = dijkstraPath;
        this.algorithmPath = algorithmPath;
        this.dijkstraDist = dijkstraDist;
        this.algorithmDist = algorithmDist;
    }
}
