package paths;

import load.GraphIO;
import model.Graph;
import model.ModelUtil;
import model.Node;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import paths.preprocessing.ReachProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
        graphIO = new GraphIO(distanceStrategy1, true);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        ReachProcessor reachProcessor = new ReachProcessor();
        List<Double> arr = reachProcessor.computeReachBound(graph);
        graphIO.saveReach(fileName, arr);
        System.out.println(arr);
    }

    @Test
    public void maltaReachSave() {
        fileName = "malta-latest.osm.pbf";
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1, true);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        ReachProcessor reachProcessor = new ReachProcessor();
        List<Double> arr = reachProcessor.computeReachBound(graph);
        graphIO.saveReach(fileName, arr);
        System.out.println(arr);
    }

    @Test
    public void estoniaReachSave() {
        fileName = "estonia-latest.osm.pbf";
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1, true);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        ReachProcessor reachProcessor = new ReachProcessor();
        List<Double> arr = reachProcessor.computeReachBound(graph);
        graphIO.saveReach(fileName, arr);
        /*List<Double> a = graphIO.loadReach(fileName);
        System.out.println(arr);
        System.out.println(a);
        Assert.assertEquals(arr, a);*/
    }

    @Test
    public void georgiaReachSave() {
        fileName = "georgia-latest.osm.pbf";
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1, true);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        ReachProcessor reachProcessor = new ReachProcessor();
        List<Double> arr = reachProcessor.computeReachBound(graph);
        graphIO.saveReach(fileName, arr);
       // System.out.println(arr);
    }

    @Test
    public void PolandSave() {
        fileName = "poland-latest.osm.pbf";
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1, true);
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
        GraphIO graphIO = new GraphIO(Util::sphericalDistance, true);
        List<Double> bounds = graphIO.loadReach(fileName);
        SSSP.setReachBounds(bounds);

    }
}
