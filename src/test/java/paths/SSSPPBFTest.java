package paths;

import load.GraphIO;
import model.Edge;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;
import load.pbfparsing.PBFParser;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;

import static org.junit.Assert.*;
import static paths.SSSP.*;
import static paths.SSSP.getHeuristicFunction;

public class SSSPPBFTest {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";
    GraphIO graphIO;

    @Before
    public void setUp() {
        PBFParser pbfParser = new PBFParser(fileName);
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        BiFunction<Node, Node, Double> distanceStrategy2 = Util::sphericalDistance;

        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
    }

    @Test
    public void testLandMarksConsistency() {
        ShortestPathResult res = SSSP.findShortestPath(600, 500, AlgorithmMode.BI_A_STAR_LANDMARKS);
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            for (Edge e : graph.getAdjList().get(i)) {
                double pForwardFrom = (getHeuristicFunction().apply(i, getTarget()) - getHeuristicFunction().apply(i, getSource())) / 2;
                double pForwardTo = (getHeuristicFunction().apply(e.to, getTarget()) - getHeuristicFunction().apply(e.to, getSource())) / 2;
                double pBackwardFrom = (getHeuristicFunction().apply(i, getSource()) - getHeuristicFunction().apply(i, getTarget())) / 2;
                double pBackwardTo = (getHeuristicFunction().apply(e.to, getSource()) - getHeuristicFunction().apply(e.to, getTarget())) / 2;
                assertEquals(e.d - pForwardFrom + pForwardTo, e.d - pBackwardTo + pBackwardFrom, 0.000000000005);
                assertEquals(pForwardFrom + pBackwardFrom, pForwardTo + pBackwardTo, 0.000000000005);
            }
        }
    }

    @Test
    public void testSingleSourceAll() {
        SSSP.seed = 1;
        ShortestPathResult res = SSSP.singleToAllPath(300);
        ShortestPathResult res2 = SSSP.findShortestPath(300, 100, AlgorithmMode.SINGLE_TO_ALL);
        System.out.println("Ahaha");
    }

    @Test
    public void testDifferenceInPath() {
        SSSP.seed = 1;
        ShortestPathResult resD = SSSP.findShortestPath(9109, 1756, AlgorithmMode.DIJKSTRA);
        ShortestPathResult resA = SSSP.findShortestPath(9109, 1756, AlgorithmMode.BI_DIJKSTRA);

        List<Double> cum_distancesD = new ArrayList<>();
        List<Double> cum_distancesA = new ArrayList<>();

        for (int i = 0; i < resD.path.size() - 1; i++) {
            int nodeIndex1 = resD.path.get(i);
            int nodeIndex2 = resD.path.get(i + 1);
            Node n1 = graph.getNodeList().get(nodeIndex1);
            Node n2 = graph.getNodeList().get(nodeIndex2);
            if (i == 0) {
                cum_distancesD.add(Util.sphericalDistance(n1, n2));
            } else {
                cum_distancesD.add(Util.sphericalDistance(n1, n2) + cum_distancesD.get(i - 1));
            }
        }
        for (int i = 0; i < resA.path.size() - 1; i++) {
            int nodeIndex1 = resA.path.get(i);
            int nodeIndex2 = resA.path.get(i + 1);
            Node n1 = graph.getNodeList().get(nodeIndex1);
            Node n2 = graph.getNodeList().get(nodeIndex2);
            if (i == 0) {
                cum_distancesA.add(Util.sphericalDistance(n1, n2));
            } else {
                cum_distancesA.add(Util.sphericalDistance(n1, n2) + cum_distancesA.get(i - 1));
            }
        }
        System.out.println();
        System.out.println(resD.path);
        System.out.println(resA.path);
        System.out.println(cum_distancesD);
        System.out.println(cum_distancesA);
        System.out.println(resD.d);
        System.out.println(resA.d);
    }

    int[] matrix;
    int testCases, i;
    double[][] runtimes;
    Map<Integer, Integer> failMap;

    @Test
    public void testAlgorithms() {
        int algorithms = 10;
        matrix = new int[algorithms];
//        List<Graph> graphs = new GraphUtil(graph).scc();
//        graph = graphs.get(0);
        SSSP.setGraph(graph);
        Landmarks lm = new Landmarks(graph);
        SSSP.setLandmarks(lm);
        lm.landmarksMaxCover(16, true);
        SSSP.setLandmarks(lm);

        List<Double> bounds = graphIO.loadReach(fileName);
        SSSP.setReachBounds(bounds);

        testCases = 80000;
        runtimes = new double[algorithms][testCases];
        i = 0;
        failMap = new HashMap<>();
        while (i < testCases) {
            seed++;
            ShortestPathResult dijkRes = SSSP.randomPath(AlgorithmMode.DIJKSTRA);
            runtimes[0][i] = dijkRes.runTime;
            double distDijk = dijkRes.d;
            List<Integer> pathDijk = dijkRes.path;

            testSingle(distDijk, pathDijk, AlgorithmMode.A_STAR, 1);
            testSingle(distDijk, pathDijk, AlgorithmMode.BI_DIJKSTRA, 2);
            testSingle(distDijk, pathDijk, AlgorithmMode.BI_A_STAR_SYMMETRIC, 3);
            testSingle(distDijk, pathDijk, AlgorithmMode.A_STAR_LANDMARKS, 4);
            testSingle(distDijk, pathDijk, AlgorithmMode.BI_A_STAR_CONSISTENT, 5);
            testSingle(distDijk, pathDijk, AlgorithmMode.BI_A_STAR_LANDMARKS, 6);
            testSingle(distDijk, pathDijk, AlgorithmMode.REACH, 7);
            testSingle(distDijk, pathDijk, AlgorithmMode.BI_REACH, 8);
            testSingle(distDijk, pathDijk, AlgorithmMode.REACH_A_STAR, 9);
            //Only interested in tests where path is atleast 100
            i++;
        }
        if (Arrays.equals(new int[algorithms], matrix)) {
            System.out.println(runtimes);
        } else fail();
    }

    private void testSingle(double distDijk, List<Integer> pathDijk, AlgorithmMode aStar, int i2) {
        ShortestPathResult aStarRes = SSSP.randomPath(aStar);
        runtimes[i2][i] = aStarRes.runTime;
        double distAstar = aStarRes.d;
        List<Integer> pathAstar = aStarRes.path;
        if (Math.abs(distAstar - distDijk) > 0.00000000001 || !pathAstar.equals(pathDijk)) {
            matrix[i2]++;
            failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size() - 1));
        }
    }
}
