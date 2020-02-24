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

    private BiFunction<Node, Node, Double> distanceStrategy = Util::flatEarthDistance;

    /**
     * The constructor of the PBFParser.
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
     * @throws FileNotFoundException If the file is not found.
     */
    public void executePBFParser() throws FileNotFoundException {
        Set<String> validNodes = findValidNodes();
        graph = new Graph(validNodes.size());
        buildGraph(validNodes);
    }

    /**
     * This method iterates through the .pbf file. It extracts all the important information
     * and converts it into our representation of a graph.
     * @param validNodes A set of all the valid nodes.
     * @throws FileNotFoundException If the file cannot be found.
     */
    private void buildGraph(Set<String> validNodes) throws FileNotFoundException {
        File file = new File(fileName);
        FileInputStream input = new FileInputStream(file);
        PbfIterator iterator = new PbfIterator(input, false);

        // Iterates over all containers in the .pbf file
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                String id = Long.toString(node.getId());
                // If a valid node is found, it will add it to the Graph.
                if (validNodes.contains(id)) {
                    constructGraphNode(node);
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
                    addEdgesGraph(way);
                }
            }
        }
        graph.setNodeList(nodeList);
    }

    /**
     * A helper method to convert from OsmWay to our representation of Edges.
     * @param way An OsmWay from the .pbf file.
     */
    private void addEdgesGraph(OsmWay way) {
        for (int i = 0; i < way.getNumberOfNodes() - 1; i++) {
            Node node1 = nodeMap.get(Long.toString(way.getNodeId(i)));
            Node node2 = nodeMap.get(Long.toString(way.getNodeId(i + 1)));
            double d = distanceStrategy.apply(node1, node2);
            graph.addEdge(node1, node2, d);
            graph.addEdge(node2, node1, d);
        }
    }

    /**
     * A helper method to convert from OsmNode to our representation of a Node.
     * @param node An OsmNode from the .pbf file.
     */
    private void constructGraphNode(OsmNode node) {
        Node n = new Node(indexCounter, node.getLatitude(), node.getLongitude());
        nodeMap.put(Long.toString(node.getId()), n);
        nodeList.add(n);
        indexCounter++;
    }

    /**
     * This method go through all the Ways can adds all USED nodes in a set.
     * @return The set of all valid nodes.
     * @throws FileNotFoundException Is thrown if file cannot be found.
     */
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

    public Graph getGraph() {
        return graph;
    }

    public void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
    }
}
