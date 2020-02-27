package paths;

import model.*;
import org.junit.Before;
import org.junit.Test;
import xml.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DijkstraTest {

    Graph graph;
    String fileName = "jelling";

    @Before
    public void setUp() {
        XMLFilter xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.setParseCordStrategy(Util::cordToInt);
        xmlGraphExtractor.executeExtractor();
        graph = xmlGraphExtractor.getGraph();
    }

    @Test
    public void testDijkstra() {
        Dijkstra.trace = true;
        double dist = Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA).d;
        Dijkstra.trace = false;
    }

    @Test
    public void compareDijkstraAStar() {
        Dijkstra.result = true;
        ShortestPathResult dijk_res = Dijkstra.sssp(graph, 2590, 1897, AlgorithmMode.DIJKSTRA);
        ShortestPathResult astar_res = Dijkstra.sssp(graph, 2590, 1897, AlgorithmMode.A_STAR);
        assertTrue(dijk_res.visitedNodes > astar_res.visitedNodes);
        assertEquals(dijk_res.d, astar_res.d, 0);
        Dijkstra.result = false;
    }
}