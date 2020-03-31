package paths;

import model.Edge;
import model.Graph;
import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;
import load.pbfparsing.PBFParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static paths.SSSP.*;
import static paths.SSSP.getHeuristicFunction;

public class SSSPPBFTest {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";

    @Before
    public void setUp() throws FileNotFoundException {
        PBFParser pbfParser = new PBFParser(fileName, false);
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        BiFunction<Node, Node, Double> distanceStrategy2 = Util::sphericalDistance;

        SSSP.setDistanceStrategy(distanceStrategy1);
        pbfParser.setDistanceStrategy(distanceStrategy2);
        try {
            pbfParser.executePBFParser();
        } catch (IOException e) {
            e.printStackTrace();
        }
        graph = pbfParser.getGraph();
        graph.extractLandmarksFarthest(16);
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
        int[] matrix = new int[7];
        graph.extractLandmarksFarthest(16);
        for (int i = 0; i < 4000; i++) {
            SSSP.seed = i;
            ShortestPathResult dijkRes = SSSP.randomPath(AlgorithmMode.DIJKSTRA);
/*
            ShortestPathResult aStarRes = SSSP.randomPath(AlgorithmMode.A_STAR);
*/
            ShortestPathResult biDijkRes = SSSP.randomPath(AlgorithmMode.BI_DIJKSTRA);
            ShortestPathResult biAStarConRes = SSSP.randomPath(AlgorithmMode.BI_A_STAR_CONSISTENT);
/*
            ShortestPathResult biAStarLandRes = SSSP.randomPath(AlgorithmMode.BI_A_STAR_LANDMARKS);
*/
/*
            ShortestPathResult biAStarSymRes = SSSP.randomPath(AlgorithmMode.BI_A_STAR_SYMMETRIC);
*/
/*
            ShortestPathResult landmarksRes = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
*/

            double distDijk = dijkRes.d;
            List<Integer> pathDijk = dijkRes.path;

          /*  double distBiLand = biAStarLandRes.d;
            List<Integer> pathBiLand = biAStarConRes.path;*/

            double distBiCon = biAStarConRes.d;
            List<Integer> pathBiCon = biAStarConRes.path;

         /*   double distAstar = aStarRes.d;
            List<Integer> pathAstar = aStarRes.path;*/

            double distBiDijk = biDijkRes.d;
            List<Integer> pathBiDijk = biDijkRes.path;

          /*  double distBiAstarSym = biAStarSymRes.d;
            List<Integer> pathBiAstarSym = biAStarSymRes.path;*/

           /* double distLandmarks = landmarksRes.d;
            List<Integer> pathLandmarks = landmarksRes.path;*/
            /*if (Math.abs(distAstar - distDijk) > 0.00000000001 || !pathAstar.equals(pathDijk)) {
                matrix[1]++;
            }*/
            if (Math.abs(distBiDijk - distDijk) > 0.00000000001 || !pathBiDijk.equals(pathDijk)) {
                matrix[2]++;
            }
          /*  if (Math.abs(distDijk - distBiAstarSym) > 0.00000000001 || !pathDijk.equals(pathBiAstarSym)) {
                matrix[3]++;
            }*/
            /*if (Math.abs(distDijk - distLandmarks) > 0.00000000001 || !pathLandmarks.equals(pathDijk)) {
                matrix[4]++;
            }*/
            if (Math.abs(distDijk - distBiCon) > 0.00000000001 || !pathBiCon.equals(pathDijk)) {
                matrix[5]++;
            }
            /*if (Math.abs(distDijk - distBiLand) > 0.00000000001 || !pathBiLand.equals(pathDijk)) {
                matrix[6]++;
            }*/
            /*if (Math.abs(distAstar - distBiDijk) > 0.00000000001 || !pathAstar.equals(pathBiDijk)) {
                matrix[1][2]++;
            }
            if (Math.abs(distAstar - distBiAstarSym) > 0.00000000001 || !pathAstar.equals(pathBiAstarSym)) {
                matrix[1][3]++;
            }
            if (Math.abs(distBiDijk - distBiAstarSym) > 0.00000000001 || !pathBiDijk.equals(pathBiAstarSym)) {
                matrix[2][3]++;
            }*/
        }
        int[] zeroMatrix = new int[7];
        assertArrayEquals(zeroMatrix, matrix);
    }
}
