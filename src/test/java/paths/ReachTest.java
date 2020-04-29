package paths;

import javafx.FXMLController;
import load.GraphIO;
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
    String fileName;
    private GraphIO graphIO;

    @Before
    public void setUp() {
     /*   fileName = "denmark-latest.osm.pbf";
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);*/
    }

    @Test
    public void denmarkReachSave() {
        fileName = "denmark-latest.osm.pbf";
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        ReachProcessor reachProcessor = new ReachProcessor();
        List<Double> arr = reachProcessor.computeReachBound(graph);
        graphIO.saveReach(fileName, arr);
        System.out.println(arr);
    }

    @Test
    public void testReachMainFunction() {
        ReachProcessor reachProcessor = new ReachProcessor();
        List<Double> arr = reachProcessor.computeReachBound(graph);
        System.out.println(arr);
    }

    @Test
    public void testBiReach() {
        GraphIO graphIO = new GraphIO(Util::sphericalDistance);
        List<Double> bounds = graphIO.loadReach(fileName);
        SSSP.setReachBounds(bounds);

    }
}
