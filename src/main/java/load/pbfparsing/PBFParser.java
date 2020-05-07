package load.pbfparsing;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import info_model.NodeInfo;
import model.Graph;
import info_model.GraphInfo;
import model.Node;
import load.pbfparsing.delegates.CollapsingStrategyFull;
import load.pbfparsing.delegates.StandardFilteringStrategy;
import load.pbfparsing.interfaces.CollapsingStrategy;
import load.pbfparsing.interfaces.FilteringStrategy;
import paths.Util;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static load.GraphIO.mapsDir;

// A tutorial for the Framework can be found at http://jaryard.com/projects/osm4j/tutorial/index.html

/**
 * The primary goal of this class is to extract information from our .pbf file and implement it into
 * our graph representation. This consists of Nodes and Edges.
 */
public class PBFParser {

    private Graph graph;
    private GraphInfo graphInfo;
    private String fileName;
    private boolean parseInfo;
    private ArrayList<Node> nodeList;
    private ArrayList<NodeInfo> nodeListInfo;
    private Map<String, Node> nodeMap;
    private int indexCounter;

    private BiFunction<Node, Node, Double> distanceStrategy;
    private FilteringStrategy filteringStrategy = new StandardFilteringStrategy();
    private CollapsingStrategy collapsingStrategy;
    private BiConsumer<String, Graph> storeTMPListener;

    /**
     * The constructor of the PBFParser.
     *
     * @param fileName the name of the file you want to extract information from.
     */
    public PBFParser(String fileName, boolean parseInfo) {
        this.fileName = fileName;
        this.parseInfo = parseInfo;
        nodeList = new ArrayList<>();
        nodeListInfo = new ArrayList<>();
        nodeMap = new HashMap<>();
        indexCounter = 0;
    }

    /**
     * This starts the execution. It runs through the file two times. First it finds all the nodes
     * used in a Way. In the second iteration it build the graph with nodes and edges.
     *
     * @throws FileNotFoundException If the file is not found.
     */
    public void executePBFParser() throws IOException {
        System.out.print("Started PBFParsing\n");
        collapsingStrategy = new CollapsingStrategyFull(distanceStrategy, parseInfo);
        Map<String, Integer> validNodes = findValidNodes();
        int amountOfValid = collapsingStrategy.getAmountOfValid(validNodes);
        graph = new Graph(amountOfValid);
        if (parseInfo) {
            graphInfo = new GraphInfo(amountOfValid);
        }

        collapsingStrategy.initSecondPass(graph, graphInfo, validNodes);
        buildGraph(validNodes);

        if (storeTMPListener != null) {
            storeTMPListener.accept(Util.trimFileTypes(fileName), graph);
        }

        System.out.print("Finished PBFParsing\n");
    }

    /**
     * This method iterates through the .pbf file. It extracts all the important information
     * and converts it into our representation of a graph.
     *
     * @param validNodesMap A map of all the valid nodes.
     * @throws FileNotFoundException If the file cannot be found.
     */
    private void buildGraph(Map<String, Integer> validNodesMap) throws FileNotFoundException {
        File file = new File(mapsDir + fileName);
        FileInputStream input = new FileInputStream(file);
        PbfIterator iterator = new PbfIterator(input, false);
        // Iterates over all containers in the .pbf file
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                String id = Long.toString(node.getId());
                Node n = constructGraphNode(node);
                // If a valid node is found, it will add it to the Graph.
                if (validNodesMap.containsKey(id)) {
                    nodeMap.put(id, n);
                    if (validNodesMap.get(id) > 1) {
                        nodeList.add(n);
                        nodeListInfo.add(getNodeInfo(n));
                        indexCounter++;
                    }
                }
            }

            if (container.getType() == EntityType.Way) {
                OsmWay way = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
                String roadValue = tags.get("highway");
                // We filter out any unwanted type of road, and continue the search through the file.
                if (filteringStrategy.shouldFilter(roadValue)) {
                    continue;
                }
                // If a valid Way is found, we iterate through it and add all the edges.
                if (way.getNumberOfNodes() > 0 && way.getNumberOfTags() > 0) {
                    collapsingStrategy.addEdgesGraph(way, nodeMap);
                }
            }
        }

        graph.setNodeList(nodeList);
        if (parseInfo) {
            graphInfo.setNodeList(nodeListInfo);
        }
    }

    private NodeInfo getNodeInfo(Node n) {
        float natureValue = 0;
        for (OsmNode naturalNode : naturalNodes) {
            double dist = getSquaredDistance(n, naturalNode.getLongitude(), naturalNode.getLatitude());
            if (dist < 0.0625) { // Closer than 50 meters to a natural node
                natureValue++;
            }
        }

        boolean closeToFuel = false;
        for (OsmNode fuelNode : fuelNodes) {
            double dist = getSquaredDistance(n, fuelNode.getLongitude(), fuelNode.getLatitude());
            if (dist < 0.0625) { // Closer than 50 meters to a fuel amenity
                closeToFuel = true;
            }
        }

        return new NodeInfo(n.index, natureValue, closeToFuel);
    }

    private double getSquaredDistance(Node n, double longitude, double latitude) {
        double lonDif = n.longitude - longitude;
        double latDif = n.latitude - latitude;
        return Math.pow(latDif, 2) + Math.pow(lonDif, 2);
    }

    /**
     * A helper method to convert from OsmNode to our representation of a Node.
     *
     * @param node An OsmNode from the .pbf file.
     */
    private Node constructGraphNode(OsmNode node) {
        double lat = Math.round(node.getLatitude() * 100000000.0) / 100000000.0;
        double lon = Math.round(node.getLongitude() * 100000000.0) / 100000000.0;
        return new Node(indexCounter, lon, lat);
    }


    private Set<OsmNode> naturalNodes = new HashSet<>();
    private Set<OsmNode> fuelNodes = new HashSet<>();

    /**
     * This method go through all the Ways can adds all USED nodes in a set.
     *
     * @return The set of all valid nodes.
     * @throws FileNotFoundException Is thrown if file cannot be found.
     */
    private Map<String, Integer> findValidNodes() throws FileNotFoundException {
        File file = new File(mapsDir + fileName);
        FileInputStream input = new FileInputStream(file);
        PbfIterator iterator = new PbfIterator(input, false);
        HashMap<String, Integer> nodeRefMap = new HashMap<>();
        for (EntityContainer container : iterator) {
            // Filter nodes used to construct graph info
            if (parseInfo && container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);
                if (tags.containsKey("amenity") && tags.get("amenity").equals("fuel")) {
                    fuelNodes.add(node);
                }
                if (tags.containsKey("natural")) {
                    naturalNodes.add(node);
                }
            }
            // Filter nodes used in graph
            if (container.getType() == EntityType.Way) {
                OsmWay way = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
                if (!tags.containsKey("highway") || filteringStrategy.shouldFilter(tags.get("highway"))) {
                    continue;
                }
                collapsingStrategy.createNodeMap(way, nodeRefMap);
            }
        }
        return nodeRefMap;
    }

    public Graph getGraph() {
        return graph;
    }

    public GraphInfo getGraphInfo() {
        return graphInfo;
    }

    public void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
    }

    public void setStoreTMPListener(BiConsumer<String, Graph> storeTMPListener) {
        this.storeTMPListener = storeTMPListener;
    }
}