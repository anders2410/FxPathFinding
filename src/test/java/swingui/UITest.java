package swingui;


import load.GraphIO;
import model.Graph;
import model.Util;
import org.junit.Before;
import org.junit.Test;
import paths.SSSP;

public class UITest {

    Graph graph;
    String fileName = "jelling";

    @Before
    public void setUp() {
        GraphIO graphIO = new GraphIO(Util::flatEarthDistance);
        graph = graphIO.loadOSMOld(fileName);
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