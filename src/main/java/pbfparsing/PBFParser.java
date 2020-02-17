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
    private String fileName;
    private ArrayList<Node> nodeList;
    private Map<String, Node> nodeMap;
    private int indexCounter;
    private String lastNdID;

    int numNodes = 0;
    int numEdges = 0;
    int numWays = 0;

    public PBFParser(String fileName) {
        this.fileName = fileName;
        nodeList = new ArrayList<>();
        nodeMap = new HashMap<>();
        indexCounter = 0;
        lastNdID = "";
    }

    public void executePBFParser() throws FileNotFoundException {
        Set<String> validNodes = findValidNodes();
        graph = new Graph(validNodes.size());
        buildGraph(validNodes);
        System.out.println("numNodes: " + numNodes);
        System.out.println("numEdges: " + numEdges);
        System.out.println("numWays: " + numWays);
    }

    private void buildGraph(Set<String> validNodes) throws FileNotFoundException {
        File file = new File(fileName);
        FileInputStream input = new FileInputStream(file);
        PbfIterator iterator = new PbfIterator(input, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                String id = Long.toString(node.getId());
                if (validNodes.contains(id)) {
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
                if (way.getNumberOfNodes() > 0 && way.getNumberOfTags() > 0) {
                    lastNdID = "";
                    numWays++;
                    addEdgesGraph(way);
                }
            }
        }
        graph.setNodeList(nodeList);
    }

    private void addEdgesGraph(OsmWay way) {
        /*if (lastNdID.equals("")) {
            lastNdID = Long.toString(way.getNodeId(0));
        }
        Node firstNode1 = nodeMap.get(lastNdID);
        Node firstNode2 = nodeMap.get(Long.toString(way.getNodeId(0)));
        System.out.println("Starting Pair: ");
        System.out.println(firstNode1.toString());
        System.out.println(firstNode2.toString());
        float firstD = Util.getNodeDistance(firstNode1, firstNode2);
        graph.addEdge(firstNode1, firstNode2, firstD);
        graph.addEdge(firstNode2, firstNode1, firstD);
        numNodesWays++;*/

        for (int i = 0; i < way.getNumberOfNodes() - 1; i++) {
            Node node1 = nodeMap.get(Long.toString(way.getNodeId(i)));
            Node node2 = nodeMap.get(Long.toString(way.getNodeId(i + 1)));
            float d = Util.getNodeDistance(node1, node2);
            graph.addEdge(node1, node2, d);
            graph.addEdge(node2, node1, d);
            numEdges += 2;
        }

        lastNdID = Long.toString(way.getNodeId(way.getNumberOfNodes() - 1));
    }

    private void constructGraphNode(OsmNode node) {
       /* String latString = Double.toString(round(node.getLatitude(), 7)).replace(".","");
        String lonString = Double.toString(round(node.getLongitude(), 7)).replace(".","");

        int lat = Integer.parseInt(latString);
        int lon = Integer.parseInt(lonString);*/
        Node n = new Node(indexCounter, node.getLatitude(), node.getLongitude());
        nodeMap.put(Long.toString(node.getId()), n);
        nodeList.add(n);
        indexCounter++;
    }

    private Set<String> findValidNodes() throws FileNotFoundException {
        File file = new File(fileName);
        FileInputStream input = new FileInputStream(file);
        PbfIterator iterator = new PbfIterator(input, false);
        HashSet<String> nodeSet = new HashSet<>();
        for (EntityContainer container : iterator) {
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

                for (int i = 0; i < way.getNumberOfNodes(); i++) {
                    nodeSet.add(Long.toString(way.getNodeId(i)));
                    numNodes++;
                }
            }
        }
        return nodeSet;
    }

    private boolean shouldFilter(String hV) {
        // TODO: Add more filters from main project.
        return hV.equals("cycleway") || hV.equals("footway") || hV.equals("path") || hV.equals("construction")
                || hV.equals("proposed") || hV.equals("raceway") || hV.equals("escape")
                || hV.equals("pedestrian") || hV.equals("track") || hV.equals("service")
                || hV.equals("bus_guideway") || hV.equals("steps")
                || hV.equals("corridor");
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public Graph getGraph() {
        return graph;
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
