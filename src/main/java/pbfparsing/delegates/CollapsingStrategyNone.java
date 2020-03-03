package pbfparsing.delegates;

import de.topobyte.osm4j.core.model.iface.OsmWay;
import model.Graph;
import model.Node;
import pbfparsing.interfaces.CollapsingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CollapsingStrategyNone implements CollapsingStrategy {

    @Override
    public void addEdgesGraph(OsmWay way,
                               Map<String, Node> allNodeMap,
                               BiFunction<Node, Node, Double> distanceStrategy,
                               Graph graph,
                               Map<String, Node> nodeMap) {

        for (int i = 0; i < way.getNumberOfNodes() - 1; i++) {
            Node node1 = nodeMap.get(Long.toString(way.getNodeId(i)));
            Node node2 = nodeMap.get(Long.toString(way.getNodeId(i + 1)));
            double d = distanceStrategy.apply(node1, node2);
            graph.addEdge(node1, node2, d);
            graph.addEdge(node2, node1, d);
        }
    }

    @Override
    public void createNodeMap(OsmWay way, HashMap<String, Integer> nodeRefMap) {
        for (int i = 0; i < way.getNumberOfNodes(); i++) {
            nodeRefMap.put(Long.toString(way.getNodeId(i)), 2);
        }
    }
}