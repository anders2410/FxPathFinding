package paths;


import load.xml.XMLFilter;
import load.xml.XMLGraphExtractor;
import model.Graph;
import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GraphUtilTest {

    Graph graph;

    @Before
    public void setUp() {
        graph = new Graph(10);

        for (int i = 0; i < 10; i++) {
            graph.addNode(new Node(i, 3, 4));

            for (int j = 0; j < i; j++) {
                graph.addEdge(i, j, j);
            }
        }
    }

    @Test
    public void testSubGraph() {
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
