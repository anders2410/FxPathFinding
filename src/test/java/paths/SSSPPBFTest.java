package paths;

import load.GraphIO;
import model.Edge;
import model.Graph;
import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;
import load.pbfparsing.PBFParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
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
        System.out.println("Finito");
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

    @Test
    public void testAlgorithms() {
        int[] matrix = new int[8];
//        List<Graph> graphs = new GraphUtil(graph).scc();
//        graph = graphs.get(0);
        SSSP.setGraph(graph);
        Landmarks lm = new Landmarks(graph);
        SSSP.setLandmarks(lm);
        lm.landmarksMaxCover(16, true);
        SSSP.setLandmarks(lm);
        try {
            double[] bounds = graphIO.loadReach(fileName);
            SSSP.setReachBounds(bounds);
        } catch (IOException e) {
            System.out.println("Load Failed in Test");
        }
        int testCases = 80000;
        double[][] runtimes = new double[8][testCases];
        int i = 0;
        Map<Integer, Integer> failMap = new HashMap<>();
        while (i < testCases) {
            seed++;
            ShortestPathResult dijkRes = SSSP.randomPath(AlgorithmMode.DIJKSTRA);
            ShortestPathResult aStarRes = SSSP.randomPath(AlgorithmMode.A_STAR);
            ShortestPathResult biDijkRes = SSSP.randomPath(AlgorithmMode.BI_DIJKSTRA);
            ShortestPathResult biAStarConRes = SSSP.randomPath(AlgorithmMode.BI_A_STAR_CONSISTENT);
            ShortestPathResult biAStarSymRes = SSSP.randomPath(AlgorithmMode.BI_A_STAR_SYMMETRIC);
            ShortestPathResult landmarksRes = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            ShortestPathResult biAStarLandRes = SSSP.randomPath(AlgorithmMode.BI_A_STAR_LANDMARKS);
            ShortestPathResult reachRes = SSSP.randomPath(AlgorithmMode.REACH);
            runtimes[0][i] = dijkRes.runTime;
            runtimes[1][i] = aStarRes.runTime;
            runtimes[2][i] = biDijkRes.runTime;
            runtimes[3][i] = biAStarConRes.runTime;
            runtimes[4][i] = biAStarSymRes.runTime;
            runtimes[5][i] = landmarksRes.runTime;
            runtimes[6][i] = biAStarLandRes.runTime;
            runtimes[7][i] = reachRes.runTime;
            double distDijk = dijkRes.d;
            List<Integer> pathDijk = dijkRes.path;

            double distBiLand = biAStarLandRes.d;
            List<Integer> pathBiLand = biAStarConRes.path;

            double distBiCon = biAStarConRes.d;
            List<Integer> pathBiCon = biAStarConRes.path;

            double distAstar = aStarRes.d;
            List<Integer> pathAstar = aStarRes.path;

            double distBiDijk = biDijkRes.d;
            List<Integer> pathBiDijk = biDijkRes.path;

            double distBiAstarSym = biAStarSymRes.d;
            List<Integer> pathBiAstarSym = biAStarSymRes.path;

            double distLandmarks = landmarksRes.d;
            List<Integer> pathLandmarks = landmarksRes.path;

            double distReach = reachRes.d;
            List<Integer> pathReach = reachRes.path;

            if (Math.abs(distAstar - distDijk) > 0.00000000001 || !pathAstar.equals(pathDijk)) {
                matrix[1]++;
                failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size()-1));
            }
            if (Math.abs(distBiDijk - distDijk) > 0.00000000001 || !pathBiDijk.equals(pathDijk)) {
                matrix[2]++;
                failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size()-1));

            }
            if (Math.abs(distDijk - distBiAstarSym) > 0.00000000001 || !pathDijk.equals(pathBiAstarSym)) {
                matrix[3]++;
                failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size()-1));

            }
            if (Math.abs(distDijk - distLandmarks) > 0.00000000001 || !pathLandmarks.equals(pathDijk)) {
                matrix[4]++;
                failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size()-1));

            }
            if (Math.abs(distDijk - distBiCon) > 0.00000000001 || !pathBiCon.equals(pathDijk)) {
                matrix[5]++;
                failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size()-1));

            }
            if (Math.abs(distDijk - distBiLand) > 0.00000000001 || !pathBiLand.equals(pathDijk)) {
                matrix[6]++;
                failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size()-1));

            }
            if (Math.abs(distDijk - distReach) > 0.00000000001 || !pathReach.toString().equals(pathDijk.toString())) {
                matrix[7]++;
                failMap.put(pathDijk.get(0), pathDijk.get(pathDijk.size()-1));

            }
            //Only interested in tests where path is atleast 100
            i++;
        }
        if (Arrays.equals(new int[8], matrix)) {
            System.out.println(runtimes);
        } else fail();
    }
}
