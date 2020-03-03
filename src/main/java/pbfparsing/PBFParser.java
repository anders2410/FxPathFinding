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
import java.util.*;
import java.util.function.BiFunction;

// A tutorial for the Framework can be found at http://jaryard.com/projects/osm4j/tutorial/index.html

/**
 * The primary goal of this class is to extract information from our .pbf file and implement it into
 * our graph representation. This consists of Nodes and Edges.
 */
public class PBFParser {

    private Graph graph;
    private String fileName;
    private ArrayList<Node> nodeList;
    private Map<String, Node> nodeMap;
    private int indexCounter;

    private BiFunction<Node, Node, Double> distanceStrategy;

    /**
     * The constructor of the PBFParser.
     *
     * @param fileName the name of the file you want to extract information from.
     */
    public PBFParser(String fileName) {
        this.fileName = fileName;
        nodeList = new ArrayList<>();
        nodeMap = new HashMap<>();
        indexCounter = 0;
    }

    /**
     * This starts the execution. It runs through the file two times. First it finds all the nodes
     * used in a Way. In the second iteration it build the graph with nodes and edges.
     *
     * @throws FileNotFoundException If the file is not found.
     */
    public void executePBFParser() throws FileNotFoundException {
        Map<String, Integer> validNodes = findValidNodes();
        graph = new Graph(validNodes.size());
        buildGraph(validNodes);
    }

    /**
     * This method iterates through the .pbf file. It extracts all the important information
     * and converts it into our representation of a graph.
     *
     * @param validNodesMap A set of all the valid nodes.
     * @throws FileNotFoundException If the file cannot be found.
     */
    private void buildGraph(Map<String, Integer> validNodesMap) throws FileNotFoundException {
        File file = new File(fileName);
        FileInputStream input = new FileInputStream(file);
        PbfIterator iterator = new PbfIterator(input, false);
        Map<String, Node> allNodeMap = new HashMap<>();
        // Iterates over all containers in the .pbf file
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                String id = Long.toString(node.getId());
                allNodeMap.put(id, constructGraphNode(node));
                // If a valid node is found, it will add it to the Graph.
                if (validNodesMap.containsKey(id)) {
                    Node n = constructGraphNode(node);
                    addNodeToGraph(id, n);
                }
            }

            if (container.getType() == EntityType.Way) {
                OsmWay way = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
                String highwayValue = tags.get("highway");
                // We filter out any unwanted type of road, and continue the search through the file.
                if (highwayValue == null) {
                    continue;
                }
                boolean filtered = shouldFilter(highwayValue);
                if (filtered) {
                    continue;
                }
                // If a valid Way is found, we iterate through it and add all the edges.
                if (way.getNumberOfNodes() > 0 && way.getNumberOfTags() > 0) {
                    addEdgesGraph(way, allNodeMap);
                }
            }
        }
        graph.setNodeList(nodeList);
    }

    private void addNodeToGraph(String id, Node n) {
        nodeMap.put(id, n);
        nodeList.add(n);
        indexCounter++;
    }

    /**
     * A helper method to convert from OsmWay to our representation of Edges.
     *
     * @param way        An OsmWay from the .pbf file.
     * @param allNodeMap
     */
    private void addEdgesGraph(OsmWay way, Map<String, Node> allNodeMap) {
        int numNodes = way.getNumberOfNodes();
        boolean finished = false;
        boolean first = true;
        String lastNodeId = null;
        double cum_Dist = 0;
        for (int i = 0; i < numNodes; i++) {
            if (finished) return;
            for (int j = 1; j < numNodes; j++) {
                if (j == numNodes - 1) {
                    finished = true;
                }
                Node node1 = nodeMap.get(Long.toString(way.getNodeId(i)));
                Node node2 = nodeMap.get(Long.toString(way.getNodeId(j)));
                if (node1 == null) {
                    break;
                }
                if (first) {
                    first = false;
                    lastNodeId = Long.toString(way.getNodeId(i));
                }
                if (node2 == null) {
                    Node intermediateNode1 = allNodeMap.get(lastNodeId);
                    Node intermediateNode2 = allNodeMap.get(Long.toString(way.getNodeId(j)));
                    lastNodeId = Long.toString(way.getNodeId(j));
                    cum_Dist += distanceStrategy.apply(intermediateNode1, intermediateNode2);
                    continue;
                }

                node1 = allNodeMap.get(Long.toString(way.getNodeId(i)));
                Node intermediate = allNodeMap.get(lastNodeId);
                node2 = allNodeMap.get(Long.toString(way.getNodeId(j)));
                cum_Dist += distanceStrategy.apply(intermediate, node2);

                graph.addEdge(node1, node2, cum_Dist);
                graph.addEdge(node2, node1, cum_Dist);
                /*graph.addEdge(node1, node2, distanceStrategy.apply(node1, node2));
                graph.addEdge(node2, node1, distanceStrategy.apply(node1, node2));*/

                cum_Dist = 0;
                lastNodeId = Long.toString(way.getNodeId(j));
                i = j;
            }
        }
    }

    /**
     * A helper method to convert from OsmNode to our representation of a Node.
     *
     * @param node An OsmNode from the .pbf file.
     */
    private Node constructGraphNode(OsmNode node) {
        double lat = Math.round(node.getLatitude() * 100000000.0) / 100000000.0;
        double lon = Math.round(node.getLongitude() * 100000000.0) / 100000000.0;
        Node n = new Node(indexCounter, lat, lon);
        return n;
    }

    /**
     * This method go through all the Ways can adds all USED nodes in a set.
     *
     * @return The set of all valid nodes.
     * @throws FileNotFoundException Is thrown if file cannot be found.
     */
    private Map<String, Integer> findValidNodes() throws FileNotFoundException {
        File file = new File(fileName);
        FileInputStream input = new FileInputStream(file);
        PbfIterator iterator = new PbfIterator(input, false);
        HashMap<String, Integer> nodeRefMap = new HashMap<>();
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
                    int referenceCount = nodeRefMap.getOrDefault(Long.toString(way.getNodeId(i)), 0);
                    nodeRefMap.put(Long.toString(way.getNodeId(i)), referenceCount + 1);
                }
            }
        }
        System.out.println("noderef size: " + nodeRefMap.size());
        nodeRefMap.values().removeIf(e -> e <= 1);
        System.out.println("noderef size: " + nodeRefMap.size());
        return nodeRefMap;
    }

    private boolean shouldFilter(String hV) {
        // TODO: Add more filters from main project.
        return hV.equals("cycleway") || hV.equals("footway") || hV.equals("path") || hV.equals("construction")
                || hV.equals("proposed") || hV.equals("raceway") || hV.equals("escape")
                || hV.equals("pedestrian") || hV.equals("track") || hV.equals("service")
                || hV.equals("bus_guideway") || hV.equals("steps")
                || hV.equals("corridor");
    }

    public Graph getGraph() {
        return graph;
    }

    public void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
    }
}
