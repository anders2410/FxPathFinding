package paths;

import load.pbfparsing.PBFParser;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ReachTest {
    Graph graph;
    String fileName = "greenland-latest.osm.pbf";

    @Before
    public void setUp() {
        PBFParser pbfParser = new PBFParser(fileName);
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
        GraphUtil g = new GraphUtil(graph);
        graph = g.scc().get(0);
    }

    @Test
    public void testReachMainFunction() {
        ReachProcessor reachProcessor = new ReachProcessor();
        List<Double> arr = reachProcessor.computeReachBound(graph);
        System.out.println(arr);
    }
}
