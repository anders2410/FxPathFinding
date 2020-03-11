package pbfparsing;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import model.Graph;
import model.Node;
import pbfparsing.delegates.CollapsingStrategyFull;
import pbfparsing.delegates.StandardFilteringStrategy;
import pbfparsing.interfaces.CollapsingStrategy;
import pbfparsing.interfaces.FilteringStrategy;

import java.io.*;
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
    private boolean preProcess;

    private BiFunction<Node, Node, Double> distanceStrategy;
    private FilteringStrategy filteringStrategy = new StandardFilteringStrategy();
    private CollapsingStrategy collapsingStrategy = new CollapsingStrategyFull();

    /**
     * The constructor of the PBFParser.
     *
     * @param fileName the name of the file you want to extract information from.
     */
    public PBFParser(String fileName, boolean preProcess) {
        this.fileName = fileName;
        this.preProcess = preProcess;
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
    public void executePBFParser() throws IOException {
        System.out.print("Started PBFParsing\n");
        Map<String, Integer> validNodes = findValidNodes();
        int sumOfValid = collapsingStrategy.getSumOfValid(validNodes);

        graph = new Graph(sumOfValid);
        buildGraph(validNodes);

        if (preProcess) {
            String name = fileName.substring(0, fileName.indexOf('.'));
            FileOutputStream fos = new FileOutputStream(name + "-node-list.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(graph.getNodeList());
            oos.close();

            FileOutputStream fos1 = new FileOutputStream(name + "-adj-list.tmp");
            ObjectOutputStream oos1 = new ObjectOutputStream(fos1);
            oos1.writeObject(graph.getAdjList());
            oos1.close();
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
        File file = new File(fileName);
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
                    collapsingStrategy.addEdgesGraph(way, distanceStrategy, graph, nodeMap, validNodesMap);
                }
            }
        }

        graph.setNodeList(nodeList);
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
                String roadValue = tags.get("highway");
                if (filteringStrategy.shouldFilter(roadValue) || !tags.containsKey("highway")) {
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

    public void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
    }
}
