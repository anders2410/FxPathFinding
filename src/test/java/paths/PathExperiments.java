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

public class PathExperiments {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";
    GraphIO graphIO;
    int[] matrix;
    int testCases, i;
    double[][] runtimes;
    Map<Integer, Integer> failMap;

    @Before
    public void setUp() {
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
    }

    @Test
    public void landmarksComparisonTest() {
        int testSize = 5000;
        SSSP.setGraph(graph);
        ShortestPathResult[][] resultArray = new ShortestPathResult[4][testSize];

        Landmarks lm = new Landmarks(graph);
        SSSP.setLandmarks(lm);
        lm.landmarksRandom(16, true);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            resultArray[0][j] = res;
        }
        lm.clearLandmarks();
        lm.landmarksFarthest(16, true);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            resultArray[1][j] = res;
        }
        lm.clearLandmarks();
        lm.landmarksAvoid(16, true);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            resultArray[2][j] = res;
        }

        lm.clearLandmarks();
        lm.landmarksMaxCover(16, true);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            resultArray[3][j] = res;
        }
    }

    @Test
    public void testAlgorithms() {
        matrix = new int[9];
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
        runtimes = new double[9][testCases];
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
            //Only interested in tests where path is atleast 100
            i++;
        }
        if (Arrays.equals(new int[9], matrix)) {
            System.out.println(Arrays.deepToString(runtimes));
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
