package paths;

import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;
import paths.preprocessing.ArcFlags;

import java.util.function.BiFunction;

public class ArcFlagsTest {
    Graph originalGraph;
    String fileName = "malta-latest.osm.pbf";
    GraphIO graphIO;

    @Before
    public void setUp() {
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1, true);
        graphIO.loadGraph(fileName);
        originalGraph = graphIO.getGraph();
        SSSP.setGraph(originalGraph);
    }

    @Test
    public void testThatSomethingWorks() {
        ArcFlags arcFlags = new ArcFlags(originalGraph);
        arcFlags.preprocess(0);
    }
}
