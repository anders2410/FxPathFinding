package swingui;


import model.Graph;
import org.junit.Before;
import org.junit.Test;
import paths.Dijkstra;
import xml.XMLFilter;
import xml.XMLGraphExtractor;

public class UITest {

    Graph graph;
    String fileName = "jelling";
    Dijkstra dijkstra = new Dijkstra();

    @Before
    public void setUp() {
        XMLFilter xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.executeExtractor();
        graph = xmlGraphExtractor.getGraph();
        // System.out.println(graph.getAdjList());
    }

    @Test
    public void runSimpleUI() {
        SimpleUI simpleUI = new SimpleUI(graph);
        simpleUI.generateUI();
        simpleUI.setVisible(true);
        Dijkstra.trace = false;
        Dijkstra.result = true;
        // while (true) {}
    }
}