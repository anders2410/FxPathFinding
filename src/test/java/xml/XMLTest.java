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
    XMLGraphExtractor xmlGraphExtractor;
    String fileName = "jelling";

    @Before
    public void setUp() {
        long startTime = System.currentTimeMillis();
        xmlFilter = new XMLFilter(fileName);
        xmlFilter.executeFilter();
        long xmlDoneTime = System.currentTimeMillis();
        System.out.println("Time to xml parse: " + (xmlDoneTime - startTime) / 1000);
        xmlGraphExtractor = new XMLGraphExtractor(fileName, xmlFilter.getValidNodes());
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
        xmlGraphExtractor.executeExtractor();
    }

    @Test
    public void testCoordinateConversion() {
        double coordinateInt = Util.cordToInt("55.7332789");
        assertEquals(557332789.0, coordinateInt, 0);
        double negCord = Util.cordToInt("-55.7332789");
        assertEquals(-557332789.0, negCord, 0);

    }

    @Test
    public void testNodeDistanceSelf() {
        Node n1 = new Node(0, 557332789, 597332789);
        Node n2 = new Node(2, 557332789, 597332789);
        double distance = Util.flatEarthDistance(n1, n2);
        double sdistance = Util.sphericalDistance(n1, n2);
        assertEquals(0.0, distance, 0);
        assertEquals(0.0, sdistance, 0);
    }

}