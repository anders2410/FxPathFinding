package pbfparsing;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import model.Graph;
import model.Node;
import model.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

// A tutorial for the Framework can be found at http://jaryard.com/projects/osm4j/tutorial/index.html
public class PBFParser {

    private Graph graph;
    private final ArrayList<Node> nodeList = new ArrayList<>();
    private Map<String, Node> nodeMap = new HashMap<>();
    private int indexCounter = 0;

    public Graph extractGraph(String filename) throws FileNotFoundException {
        Set<String> validNodes = findValidNodes(filename);
        graph = new Graph(validNodes.size());
        buildGraph(filename, validNodes);
        graph.setNodeList(nodeList);
        return graph;
    }

    private void buildGraph(String filename, Set<String> validNodes) throws FileNotFoundException {
        File file = new File(filename);
        FileInputStream input = new FileInputStream(file);
        Iterator<EntityContainer> iterator = new PbfIterator(input, false);

        while (iterator.hasNext()) {
            EntityContainer container = iterator.next();
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                if (validNodes.contains(Long.toString(node.getId()))) {
                    constructGraphNode(node);
                }
            }
            if (container.getType() == EntityType.Way) {
                OsmWay way = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
                String highwayValue = tags.get("highway");
                if (highwayValue == null) {
                    continue;
                }
                boolean filtered = shouldFilter(highwayValue);
                if (filtered) {
                    continue;
                }
                addEdgesGraph(way);
            }
        }
    }

    private void addEdgesGraph(OsmWay way) {
        for (int i = 0; i < way.getNumberOfNodes()-1; i++) {
            Node node1 = nodeMap.get(Long.toString(way.getNodeId(i)));
            Node node2 = nodeMap.get(Long.toString(way.getNodeId(i + 1)));
            float d = Util.getNodeDistance(node1, node2);
            graph.addEdge(node1, node2, d);
            graph.addEdge(node2, node1, d);
        }
    }

    private void constructGraphNode(OsmNode node) {
        String latString = Double.toString(round(node.getLatitude(), 7)).replace(".","");
        String lonString = Double.toString(round(node.getLongitude(), 7)).replace(".","");
        // String latSub = latString.substring(0, latString.length() - 8);
        // String lonSub = lonString.substring(0, lonString.length() - 8);

            int lat = Integer.parseInt(latString);
            int lon = Integer.parseInt(lonString);
            Node n = new Node(indexCounter, lat, lon);
            indexCounter++;
            nodeList.add(n);
            nodeMap.put(Long.toString(node.getId()), n);

    }

    private Set<String> findValidNodes(String filename) throws FileNotFoundException {
        File file = new File(filename);
        FileInputStream input = new FileInputStream(file);
        Iterator<EntityContainer> iterator = new PbfIterator(input, false);
        HashSet<String> nodeSet = new HashSet<>();
        while (iterator.hasNext()) {
            EntityContainer container = iterator.next();
            if (container.getType() == EntityType.Way) {
                OsmWay way = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
                String highwayValue = tags.get("highway");
                if (highwayValue == null) {
                    continue;
                }
                boolean filtered = shouldFilter(highwayValue);
                if (filtered) {
                    continue;
                }
                int nodeNum = way.getNumberOfNodes();
                for (int i = 0; i < nodeNum; i++) {
                    nodeSet.add(Long.toString(way.getNodeId(i)));
                }
            }
        }
        return nodeSet;
    }

    private boolean shouldFilter(String highwayValue) {
        // TODO: Add more filters from main project.
        return highwayValue.equals("cycleway") || highwayValue.equals("footway") || highwayValue.equals("path")
                || highwayValue.equals("proposed") || highwayValue.equals("raceway") || highwayValue.equals("escape")
                || highwayValue.equals("pedestrian") || highwayValue.equals("track")
                || highwayValue.equals("bus_guideway") || highwayValue.equals("steps")
                || highwayValue.equals("corridor");
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /*private void TestSpeedRaw(String filename) throws FileNotFoundException {
        File file = new File(filename);
        FileInputStream input = new FileInputStream(file);
        Iterator<EntityContainer> iterator = new PbfIterator(input, false);
        long starttime = System.currentTimeMillis();
        while (iterator.hasNext()) {
            EntityContainer container = iterator.next();
        }
        long endtime = System.currentTimeMillis();
        System.out.println("Time to parse denmark.pbf once : " + (endtime - starttime) + " ms");

        *//*File file1 = new File("denmark-latest.osm");
        input = new FileInputStream(file1);
        OsmXmlIterator iterator1 = new OsmXmlIterator(input, false);
        long starttime1 = System.currentTimeMillis();
        while (iterator1.hasNext()) {
            EntityContainer container = iterator1.next();
        }
        long endtime1 = System.currentTimeMillis();
        System.out.println("Time to parse denmark.xml once : " + (endtime1 - starttime1) + " ms");*//*
    }*/
}
