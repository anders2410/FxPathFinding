package load.pbfparsing.delegates;

import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import info_model.EdgeInfo;
import model.Graph;
import info_model.GraphInfo;
import model.Node;
import load.pbfparsing.interfaces.CollapsingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CollapsingStrategyFull implements CollapsingStrategy {
    boolean oneWayFlag = true;

    BiFunction<Node, Node, Double> distanceStrategy;

    Graph graph;
    GraphInfo graphInfo;
    Map<String, Integer> validNodesMap;

    public CollapsingStrategyFull(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
    }

    @Override
    public void initSecondPass(Graph graph, GraphInfo graphInfo, Map<String, Integer> validNodes) {
        this.graph = graph;
        this.graphInfo = graphInfo;
        this.validNodesMap = validNodes;
    }

    @Override
    public void addEdgesGraph(OsmWay way, Map<String, Node> nodeMap) {
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
                if (validNodesMap.get(Long.toString(way.getNodeId(i))) == null || validNodesMap.get(Long.toString(way.getNodeId(i))) < 2) {
                    break;
                }
                if (first) {
                    first = false;
                    lastNodeId = Long.toString(way.getNodeId(i));
                }
                if (validNodesMap.get(Long.toString(way.getNodeId(j))) == null || validNodesMap.get(Long.toString(way.getNodeId(j))) < 2) {
                    Node intermediateNode1 = nodeMap.get(lastNodeId);
                    Node intermediateNode2 = nodeMap.get(Long.toString(way.getNodeId(j)));
                    lastNodeId = Long.toString(way.getNodeId(j));
                    cum_Dist += distanceStrategy.apply(intermediateNode1, intermediateNode2);
                    continue;
                }

                Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
                Node node1 = nodeMap.get(Long.toString(way.getNodeId(i)));
                Node intermediate = nodeMap.get(lastNodeId);
                Node node2 = nodeMap.get(Long.toString(way.getNodeId(j)));
                cum_Dist += distanceStrategy.apply(intermediate, node2);

                graph.addEdge(node1, node2, cum_Dist);
                String maxSpeedString = tags.get("maxspeed");

                int maxSpeed = -1;
                if (maxSpeedString != null) {
                    if (maxSpeedString.contains("rural")) {         // Primært DK, Rumænien og Rusland
                        maxSpeed = 80;
                    } else if (maxSpeedString.contains("urban")) {  // Primært DK, Rumænien og Rusland
                        maxSpeed = 50;
                    } else if (maxSpeedString.equals("none")) {     // Tyske motorveje
                        maxSpeed = 140;
                    } else {
                        try {
                            maxSpeed = Integer.parseInt(maxSpeedString);
                        } catch (NumberFormatException e) {
                            System.out.println("Couldn't parse: " + maxSpeedString + " to integer.");
                        }
                    }
                }
                EdgeInfo edgeInfo = new EdgeInfo(node1.index, node2.index, maxSpeed);
                graphInfo.addEdge(edgeInfo);

                // Flag to decide whether to use oneWay roads.
                if (oneWayFlag) {
                    String roadValue = tags.get("oneway");
                    String roundabout = tags.get("junction");
                    if (roadValue == null || !roadValue.equals("yes")) {
                        if (roundabout == null || !roundabout.equals("roundabout")) {
                            graph.addEdge(node2, node1, cum_Dist);
                            graphInfo.addEdge(new EdgeInfo(edgeInfo.getTo(), edgeInfo.getFrom(), maxSpeed));
                        }
                    }
                } else {
                    graph.addEdge(node2, node1, cum_Dist);
                    graphInfo.addEdge(new EdgeInfo(edgeInfo.getTo(), edgeInfo.getFrom(), maxSpeed));
                }

                cum_Dist = 0;
                lastNodeId = Long.toString(way.getNodeId(j));
                i = j;
            }
        }
    }

    @Override
    public void createNodeMap(OsmWay way, HashMap<String, Integer> nodeRefMap) {
        for (int i = 0; i < way.getNumberOfNodes(); i++) {
            int referenceCount = nodeRefMap.getOrDefault(Long.toString(way.getNodeId(i)), 0);
            nodeRefMap.put(Long.toString(way.getNodeId(i)), referenceCount + 1);
        }
    }

    @Override
    public int getSumOfValid(Map<String, Integer> validNodes) {
        return validNodes.values().stream().map(integer -> {
            if (integer > 1) {
                return 1;
            } else {
                return 0;
            }
        }).reduce(0, Integer::sum);
    }
}