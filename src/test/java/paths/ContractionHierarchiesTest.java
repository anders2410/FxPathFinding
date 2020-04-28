package paths;

import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.function.BiFunction;

public class ContractionHierarchiesTest {
    Graph originalGraph;
    String fileName = "malta-latest.osm.pbf";
    private GraphIO graphIO;

    @Before
    public void setUp() {
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        originalGraph = graphIO.getGraph();
        SSSP.setGraph(originalGraph);

        System.out.println("------------------------------ ORIGINAL GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + originalGraph.getNodeAmount());
        System.out.println("Number of edges: " + originalGraph.getEdgeAmount());
    }

    @Test
    public void testContractionHierarchiesMainFunction() {
        ContractionHierarchies contractionHierarchies = new ContractionHierarchies(originalGraph);
        ContractionHierarchiesResult contractionHierarchiesResult = contractionHierarchies.preprocess();
        SSSP.setContractionHierarchiesResult(contractionHierarchiesResult);

        System.out.println("------------------------------ AUGMENTED GRAPH ----------------------------------------------------");
        System.out.println("Number of nodes: " + contractionHierarchiesResult.getGraph().getNodeAmount());
        System.out.println("Number of edges: " + contractionHierarchiesResult.getGraph().getEdgeAmount());


        System.out.println("------------------------------ TESTING VS. DIJKSTRA -----------------------------------------------");
        ShortestPathResult dijkstraResult;
        ShortestPathResult CHResult;

        for (int i = 0; i < 10; i++) {
            SSSP.setGraph(originalGraph);
            dijkstraResult = SSSP.randomPath(AlgorithmMode.DIJKSTRA);

            SSSP.setGraph(contractionHierarchiesResult.getGraph());
            CHResult = SSSP.randomPath(AlgorithmMode.CONTRACTION_HIERARCHIES);

            System.out.println(dijkstraResult.path);
            System.out.println(CHResult.path);
            System.out.println("Dijkstra distance: " + dijkstraResult.d);
            System.out.println("CH distance: " + CHResult.d);

            SSSP.seed++;
        }
    }
}
