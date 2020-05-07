package paths;

import load.GraphIO;
import model.Edge;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;
import load.pbfparsing.PBFParser;
import paths.preprocessing.ContractionHierarchies;
import paths.preprocessing.ContractionHierarchiesResult;
import paths.preprocessing.LandmarkMode;
import paths.preprocessing.Landmarks;

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
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;

        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1, true);
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
        SSSP.setGraph(graph);
        Landmarks lm = new Landmarks(graph);
        SSSP.setLandmarks(lm);
/*
        lm.landmarksMaxCover(16, true);
*/
        new GraphIO(getDistanceStrategy(), true).loadLandmarks(fileName, LandmarkMode.MAXCOVER, lm);
        SSSP.setLandmarks(lm);
        List<Double> bounds = graphIO.loadReach(fileName);
        SSSP.setReachBounds(bounds);
        ShortestPathResult resD = SSSP.findShortestPath(8702, 11049, AlgorithmMode.DIJKSTRA);
        ShortestPathResult resA = SSSP.findShortestPath(8702, 11049, AlgorithmMode.BI_A_STAR_CONSISTENT);
        ShortestPathResult resB = SSSP.findShortestPath(8702, 11049, AlgorithmMode.BI_REACH_A_STAR);
        ShortestPathResult resC = SSSP.findShortestPath(8702, 11049, AlgorithmMode.BI_REACH_LANDMARKS);


        System.out.println(resD.path);
        System.out.println(resA.path);
        System.out.println(resB.path);
        System.out.println(resC.path);

        System.out.println(resD.d);
        System.out.println(resA.d);
        System.out.println(resB.d);
        System.out.println(resC.d);

    }

    int[] matrix;
    int testCases, i;
    double[][] runtimes;
    Map<Integer, Integer> failMap;

    @Test
    public void testAlgorithms() {
        int algorithms = 14;
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

        ContractionHierarchies contractionHierarchies = new ContractionHierarchies(graph);
        ContractionHierarchiesResult contractionHierarchiesResult = contractionHierarchies.preprocess();
        SSSP.setContractionHierarchiesResult(contractionHierarchiesResult);
        testCases = 1000;
        runtimes = new double[algorithms][testCases];

        i = 0;
        failMap = new HashMap<>();
        seed = 0;
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
            testSingle(distDijk, pathDijk, AlgorithmMode.BI_REACH_A_STAR, 10);
            testSingle(distDijk, pathDijk, AlgorithmMode.REACH_LANDMARKS, 11);
            testSingle(distDijk, pathDijk, AlgorithmMode.BI_REACH_LANDMARKS, 12);
            testSingle(distDijk, pathDijk, AlgorithmMode.CONTRACTION_HIERARCHIES, 13);

            //Only interested in tests where path is atleast 100
            i++;
        }
        if (Arrays.equals(new int[algorithms], matrix)) {
            System.out.println(Arrays.deepToString(runtimes));
        } else fail();
    }

    private void testSingle(double distDijk, List<Integer> pathDijk, AlgorithmMode aStar, int i2) {
        ShortestPathResult aStarRes = SSSP.randomPath(aStar);
        runtimes[i2][i] = aStarRes.runTime;
        double distAstar = aStarRes.d;
        List<Integer> pathAstar = aStarRes.path;
        if (Math.abs(distAstar - distDijk) > 0.00000000001 || !pathAstar.equals(pathDijk)) {
            System.out.println(aStar + ": " + pathDijk.get(0) + " -> " + pathDijk.get(pathDijk.size() - 1));
            matrix[i2]++;
            failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size() - 1));
        }
    }
}
