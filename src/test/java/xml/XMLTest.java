package xml;

import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class XMLTest {

    XMLFilter xmlFilter;
    String fileName = "jelling";

    @Before
    public void setUp() {
        long startTime = System.currentTimeMillis();
        xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        long xmlDoneTime = System.currentTimeMillis();
        System.out.println("Time to xml parse: " + (xmlDoneTime - startTime) / 1000);
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.executeExtractor();
        long graphExtractDoneTime = System.currentTimeMillis();
        System.out.println("Time to graph extract: " + (graphExtractDoneTime - xmlDoneTime) / 1000);
    }

    @Test
    public void testFilter() {
        Set<String> nodeCounterMap = xmlFilter.getValidNodes();
        System.out.println("Node set has size: " + nodeCounterMap.size());
    }

    @Test
    public void testExtractor() {
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
        xmlGraphExtractor.executeExtractor();
    }

    @Test
    public void testCoordinateConversion() {
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor("jelling", new HashSet<>());
        int coordinateInt = Util.cordToInt("55.7332789");
        assertEquals(557332789, coordinateInt);
        int negCord = Util.cordToInt("-55.7332789");
        assertEquals(-557332789, negCord);

    }

    @Test
    public void testNodeDistance() {
        XMLGraphExtractor xmlGraphExtractor = new XMLGraphExtractor("jelling", new HashSet<>());

        Node n1 = new Node(0, 557332789, 597332789);
        Node n2 = new Node(2, 557332789, 597332789);
        float distance = Util.getNodeDistance(n1, n2);
        assertEquals(0.0, distance);
    }

}