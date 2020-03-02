package paths;

import model.Graph;
import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;
import pbfparsing.PBFParser;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.function.BiFunction;

import static org.junit.Assert.assertArrayEquals;

public class DijkstraPBFTest {
    Graph graph;
    String fileName = "isle-of-man-latest.osm.pbf";

    @Before
    public void setUp() throws FileNotFoundException {
        PBFParser pbfParser = new PBFParser(fileName);
        BiFunction<Node, Node, Double> distanceStrategy = Util::sphericalDistance;
        pbfParser.setDistanceStrategy(distanceStrategy);
        pbfParser.executePBFParser();
        graph = pbfParser.getGraph();
    }

    @Test
    public void testAlgorithms() {
        int[][] matrix = new int[4][4];
        for (int i = 0; i < 100; i++) {
            double distDijk = Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA).d;
            double distAstar = Dijkstra.randomPath(graph, AlgorithmMode.A_STAR).d;
            double distBiDijk = Dijkstra.randomPath(graph, AlgorithmMode.BI_DIJKSTRA).d;
            double distBiAstar = Dijkstra.randomPath(graph, AlgorithmMode.BI_A_STAR).d;
            Dijkstra.seed = i;

            if (distDijk != distAstar) {
                matrix[0][1]++;
            }
            if (distDijk != distBiDijk) {
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
            }
        }

        System.out.println(Arrays.deepToString(matrix));

        int[][] zeroMatrix = new int[4][4];
        assertArrayEquals(zeroMatrix, matrix);
    }
}
