package paths;

import load.pbfparsing.PBFParser;
import model.Graph;
import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.function.BiFunction;

public class ContractionHierarchiesTest {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";

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

        System.out.println("------------------------------ ORIGINAL GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + graph.getNodeAmount());
        System.out.println("Number of edges: " + graph.getEdgeAmount());
    }

    @Test
    public void testContractionHierarchiesMainFunction() {
        ContractionHierarchies contractionHierarchies = new ContractionHierarchies(graph);
        Graph CHGraph = contractionHierarchies.preprocess();
        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + CHGraph.getNodeAmount());
        System.out.println("Number of edges: " + CHGraph.getEdgeAmount());


    }
}
