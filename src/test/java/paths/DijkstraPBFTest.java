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
import java.util.function.BiFunction;

import static org.junit.Assert.assertArrayEquals;

public class DijkstraPBFTest {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";

    @Before
    public void setUp() throws FileNotFoundException {
        PBFParser pbfParser = new PBFParser(fileName);
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        BiFunction<Node, Node, Double> distanceStrategy2 = Util::sphericalDistance;

        Dijkstra.setDistanceStrategy(distanceStrategy1);
        pbfParser.setDistanceStrategy(distanceStrategy2);
        pbfParser.executePBFParser();
        graph = pbfParser.getGraph();
    }

    @Test
    public void testDifferenceInPath() {
        /*for (int i = 0; i < 99999; i++) {
            Dijkstra.seed = i;
            ShortestPathResult resD = Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA);
            ShortestPathResult resA = Dijkstra.randomPath(graph, AlgorithmMode.A_STAR);
            if (resD.path.size() < 10 && resA.d != resD.d) {
                System.out.println("Found good seed: " + i);
            }
        }*/
        Dijkstra.seed = 1183;
        ShortestPathResult resD = Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA);
        ShortestPathResult resA = Dijkstra.randomPath(graph, AlgorithmMode.A_STAR);
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
        System.out.println(resA.path);
        System.out.println(resD.path);
        System.out.println(cum_distancesA);
        System.out.println(cum_distancesD);
        System.out.println(resA.d);
        System.out.println(resD.d);
    }

    @Test
    public void testAlgorithms() {
        int[][] matrix = new int[4][4];
        for (int i = 0; i < 1500; i++) {
            Dijkstra.seed = i;
            double distDijk = Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA).d;
            double distAstar = Dijkstra.randomPath(graph, AlgorithmMode.A_STAR).d;
/*          double distBiDijk = Dijkstra.randomPath(graph, AlgorithmMode.BI_DIJKSTRA).d;
            double distBiAstar = Dijkstra.randomPath(graph, AlgorithmMode.BI_A_STAR).d;*/
            if (distDijk != distAstar) {
                if (distAstar < distDijk) {
                    System.out.println(distAstar);
                    System.out.println(distDijk);
                    matrix[0][1]++;
                } else {
                /*System.out.println(Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA).d);
                System.out.println(Dijkstra.randomPath(graph, AlgorithmMode.A_STAR).d);

                System.out.println(Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA).path);
                System.out.println(Dijkstra.randomPath(graph, AlgorithmMode.A_STAR).path);*/
                    matrix[0][0]++;
                }
            }
           /* if (distDijk != distBiDijk) {
                matrix[0][2]++;
            }
            if (distDijk != distBiAstar) {
                matrix[0][3]++;
            }
            if (distAstar != distBiDijk) {
                matrix[1][2]++;
            }
            if (distAstar != distBiAstar) {
                matrix[1][3]++;
            }
            if (distBiDijk != distBiAstar) {
                matrix[2][3]++;
            }*/
        }

        System.out.println(Arrays.deepToString(matrix));

        int[][] zeroMatrix = new int[4][4];
        assertArrayEquals(zeroMatrix, matrix);
    }
}
