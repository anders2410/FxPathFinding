package pbfparsing.interfaces;

import de.topobyte.osm4j.core.model.iface.OsmWay;
import model.Graph;
import model.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public interface CollapsingStrategy {
    void addEdgesGraph(OsmWay way, Map<String, Node> allNodeMap, BiFunction<Node, Node, Double> distanceStrategy, Graph graph, Map<String, Node> nodeMap);

    void createNodeMap(OsmWay way, HashMap<String, Integer> nodeRefMap);
}
