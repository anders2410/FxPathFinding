package paths;

import model.*;
import org.junit.Before;
import org.junit.Test;
import xml.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SSSPTest {

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
        SSSP.setGraph(graph);
    }

    @Test
    public void testDijkstra() {
        SSSP.trace = true;
        double dist = SSSP.randomPath(AlgorithmMode.DIJKSTRA).d;
        SSSP.trace = false;
    }

    @Test
    public void compareDijkstraAStar() {
        SSSP.traceResult = true;
        ShortestPathResult dijk_res = SSSP.findShortestPath(2590, 1897, AlgorithmMode.DIJKSTRA);
        ShortestPathResult astar_res = SSSP.findShortestPath(2590, 1897, AlgorithmMode.A_STAR);
        assertTrue(dijk_res.visitedNodes > astar_res.visitedNodes);
        assertEquals(dijk_res.d, astar_res.d, 0);
        SSSP.traceResult = false;
    }
}