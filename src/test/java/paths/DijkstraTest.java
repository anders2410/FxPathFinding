package paths;

import model.*;
import org.junit.Before;
import org.junit.Test;
import paths.AlgorithmMode;
import paths.Dijkstra;
import xml.XMLFilter;
import xml.XMLGraphExtractor;

import static org.junit.Assert.assertEquals;

public class DijkstraTest {

    Graph graph;
    String fileName = "jelling";

    @Before
    public void setUp() {
        XMLFilter xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.executeExtractor();
        graph = xmlGraphExtractor.getGraph();
    }

    @Test
    public void testDijkstra() {
        Dijkstra.trace = true;
        float dist = Dijkstra.randomPath(graph, AlgorithmMode.DIJKSTRA).d;
        Dijkstra.trace = false;
    }

    @Test
    public void compareDijkstraAStar() {
        Dijkstra.result = true;
        float dist_dijk = Dijkstra.sssp(graph, 2590, 1897, AlgorithmMode.DIJKSTRA).d;
        float dist_astar = Dijkstra.sssp(graph, 2590, 1897, AlgorithmMode.A_STAR_DIST).d;
        assertEquals(dist_dijk, dist_astar, 0);
        Dijkstra.result = false;
    }
}