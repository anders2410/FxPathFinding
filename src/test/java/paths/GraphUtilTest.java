package paths;


import load.GraphIO;
import model.Edge;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphUtilTest {

    Graph graph;

    @Before
    public void setUp() {
        GraphIO graphIO;
        String fileName = "malta-latest.osm.pbf";
        SSSP.setDistanceStrategy(Util::sphericalDistance);
        graphIO = new GraphIO(Util::sphericalDistance, true);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
    }

    /*@Test
    public void testInGoingEdgesMap() {
        GraphUtil graphUtil = new GraphUtil(graph);
        Map<Integer, List<Node>> m = graphUtil.getInDegreeNodeMap();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            for (Edge e : graph.getAdjList().get(i)) {
                assertTrue(m.get(e.to).contains(graph.getNodeList().get(i)));
            }
        }
    }*/

    @Test
    public void testSubGraph() {
        graph = new Graph(10);

        for (int i = 0; i < 10; i++) {
            graph.addNode(new Node(i, 3, 4));

            for (int j = 0; j < i; j++) {
                graph.addEdge(i, j, j);
            }
        }
        GraphUtil graphUtil = new GraphUtil(graph);
        Graph subGraph = graphUtil.subGraph(Arrays.asList(1, 2, 3));
        subGraph.printAdjList();
        Graph subGraph2 = graphUtil.subGraph(Arrays.asList(1, 2, 4, 5));
        subGraph2.printAdjList();
        assertEquals(subGraph.getEdgeAmount(), 3);
        assertEquals(subGraph.getNodeAmount(), 3);
        assertEquals(subGraph2.getEdgeAmount(), 6);
        assertEquals(subGraph2.getNodeAmount(), 4);
        assertEquals(subGraph2.getNodeList().get(3).index, 3);
        assertEquals(subGraph.getNodeList().get(1).index, 1);
        assertEquals(subGraph2.getAdjList().get(3).get(2).d, graph.getAdjList().get(5).get(4).d, 0);
    }

}
