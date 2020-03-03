package pbfparsing.delegates;

import de.topobyte.osm4j.core.model.iface.OsmWay;
import model.Graph;
import model.Node;
import pbfparsing.interfaces.CollapsingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CollapsingStrategyFull implements CollapsingStrategy {
    @Override
    public void addEdgesGraph(OsmWay way, Map<String, Node> allNodeMap, BiFunction<Node, Node, Double> distanceStrategy,
                               Graph graph, Map<String, Node> nodeMap) {
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
}
