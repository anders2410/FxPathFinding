package load.pbfparsing.interfaces;

import de.topobyte.osm4j.core.model.iface.OsmWay;
import model.Graph;
import info_model.GraphInfo;
import model.Node;

import java.util.HashMap;
import java.util.Map;

public interface CollapsingStrategy {

    void initSecondPass(Graph graph, GraphInfo graphInfo, Map<String, Integer> validNodes);

    void addEdgesGraph(OsmWay way, Map<String, Node> nodeMap);

    void createNodeMap(OsmWay way, HashMap<String, Integer> nodeRefMap);

    int getSumOfValid(Map<String, Integer> validNodes);
}