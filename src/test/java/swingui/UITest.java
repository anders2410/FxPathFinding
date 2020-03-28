package swingui;


import load.GraphImport;
import model.Graph;
import model.Util;
import org.junit.Before;
import org.junit.Test;
import paths.SSSP;
import load.xml.XMLFilter;
import load.xml.XMLGraphExtractor;

public class UITest {

    Graph graph;
    String fileName = "jelling";

    @Before
    public void setUp() {
        GraphImport graphImport = new GraphImport(Util::flatEarthDistance);
        graph = graphImport.loadOSMOld(fileName);
    }

    @Test
    public void runSimpleUI() {
        SimpleUI simpleUI = new SimpleUI(graph);
        simpleUI.generateUI();
        simpleUI.setVisible(true);
        SSSP.trace = false;
        SSSP.traceResult = true;
        while (true) {}
    }
}