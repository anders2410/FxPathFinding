package paths;

import model.Graph;
import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;
import pbfparsing.PBFParser;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.Assert.assertArrayEquals;

public class SSSPPBFTest {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";

    @Before
    public void setUp() throws FileNotFoundException {
        PBFParser pbfParser = new PBFParser(fileName);
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        BiFunction<Node, Node, Double> distanceStrategy2 = Util::sphericalDistance;

        SSSP.setDistanceStrategy(distanceStrategy1);
        pbfParser.setDistanceStrategy(distanceStrategy2);
        pbfParser.executePBFParser();
        graph = pbfParser.getGraph();
    }

    @Test
    public void testSingleSourceAll() {
        SSSP.seed = 1;
        ShortestPathResult res = SSSP.singleToAllPath(graph,300);
        System.out.println("Finito");
    }

    @Test
    public void testDifferenceInPath() {
        SSSP.seed = 1;
        ShortestPathResult resD = SSSP.sssp(graph, 6318, 7717, AlgorithmMode.BI_DIJKSTRA);
        ShortestPathResult resA = SSSP.randomPath(graph, AlgorithmMode.BI_A_STAR_SYMMETRIC);
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
        int[][] matrix = new int[4][4];
        for (int i = 0; i < 4000; i++) {
            SSSP.seed = i;
            ShortestPathResult dijkRes = SSSP.randomPath(graph, AlgorithmMode.DIJKSTRA);
            ShortestPathResult aStarRes = SSSP.randomPath(graph, AlgorithmMode.A_STAR);
            ShortestPathResult biDijkRes = SSSP.randomPath(graph, AlgorithmMode.BI_DIJKSTRA);
            ShortestPathResult biAStarRes = SSSP.randomPath(graph, AlgorithmMode.BI_A_STAR_SYMMETRIC);

            double distDijk = dijkRes.d;
            List<Integer> pathDijk = dijkRes.path;

            double distAstar = aStarRes.d;
            List<Integer> pathAstar = aStarRes.path;

            double distBiDijk = biDijkRes.d;
            List<Integer> pathBiDijk = biDijkRes.path;

            double distBiAstar = biAStarRes.d;
            List<Integer> pathBiAstar = biAStarRes.path;
            if (Math.abs(distAstar - distDijk) > 0.00000000001 || !pathAstar.equals(pathDijk)) {
                System.out.println("--------Dijk vs Astar -----------");
                System.out.println(distAstar);
                System.out.println(distDijk);
                System.out.println(pathAstar);
                System.out.println(pathDijk);
                matrix[0][1]++;
            }
            if (Math.abs(distBiDijk - distDijk) > 0.00000000001 || !pathBiDijk.equals(pathDijk)) {
                System.out.println("--------BiDijk vs Dijk -----------");
                System.out.println(distBiDijk);
                System.out.println(distDijk);
                System.out.println(pathDijk);
                System.out.println(pathBiDijk);
                matrix[0][2]++;
            }
            if (Math.abs(distDijk - distBiAstar) > 0.00000000001 || !pathDijk.equals(pathBiAstar)) {
                System.out.println(i);
                matrix[0][3]++;
            }
            if (Math.abs(distAstar - distBiDijk) > 0.00000000001 || !pathAstar.equals(pathBiDijk)) {
                System.out.println("--------BiDijk vs Astar -----------");
                System.out.println(distAstar);
                System.out.println(distBiDijk);
                System.out.println(pathAstar);
                System.out.println(pathBiDijk);
                matrix[1][2]++;
            }
            if (Math.abs(distAstar - distBiAstar) > 0.00000000001 || !pathAstar.equals(pathBiAstar)) {
                matrix[1][3]++;
            }
            if (Math.abs(distBiDijk - distBiAstar) > 0.00000000001 || !pathBiDijk.equals(pathBiAstar)) {
                matrix[2][3]++;
            }
        }
        int[][] zeroMatrix = new int[4][4];
        System.out.println(Arrays.deepToString(matrix));
        assertArrayEquals(zeroMatrix, matrix);
    }
}
