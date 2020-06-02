package paths.generator;

import info_model.NodeInfo;
import model.Edge;
import info_model.EdgeInfo;
import model.Node;
import paths.ABDir;
import paths.SSSP;
import paths.strategy.EdgeWeightStrategy;

import java.util.List;
import java.util.function.Function;

import static paths.ABDir.A;

public class EdgeWeightGenerator {

    public static EdgeWeightStrategy getDistanceWeights() {
        return new EdgeWeightStrategy() {
            @Override
            public double getWeight(Edge edge, ABDir dir) {
                return edge.d;
            }

            @Override
            public double lowerBoundDistance(Node node1, Node node2) {
                return SSSP.getDistanceStrategy().apply(node1, node2);
            }

            @Override
            public String getFileSuffix() {
                return "";
            }
        };
    }

    public static EdgeWeightStrategy getMaxSpeedTime() {
        return new EdgeWeightStrategy() {
            @Override
            public double getWeight(Edge e, ABDir dir) {
                EdgeInfo info = getEdgeInfo(e, dir);
                return e.d / (info.getMaxSpeed() == -1 ? 50 : info.getMaxSpeed());
            }

            @Override
            public double lowerBoundDistance(Node node1, Node node2) {
                return SSSP.getDistanceStrategy().apply(node1, node2) / 130;
            }

            @Override
            public String getFileSuffix() {
                return "-speed";
            }
        };
    }

    private static EdgeInfo getEdgeInfo(Edge e, ABDir dir) {
        return SSSP.getGraphInfo().getEdge(dir == A && !SSSP.reverseMe ? e : e.getReverse());
    }

    private static float maxNature = -1;

    public static EdgeWeightStrategy getNatural() {
        return new EdgeWeightStrategy() {
            @Override
            public double getWeight(Edge e, ABDir dir) {
                EdgeInfo edgeInfo = getEdgeInfo(e, dir);
                NodeInfo nodeInfo = SSSP.getGraphInfo().getNodeList().get(edgeInfo.getTo());
                if (maxNature == -1) {
                    for (NodeInfo info : SSSP.getGraphInfo().getNodeList()) {
                        if (info.getNatureValue() > maxNature) {
                            maxNature = info.getNatureValue();
                        }
                    }
                }
                return e.d * (0.4 + 0.6 * (maxNature - nodeInfo.getNatureValue()) / maxNature);
            }

            @Override
            public double lowerBoundDistance(Node node1, Node node2) {
                return SSSP.getDistanceStrategy().apply(node1, node2) * 0.4;
            }

            @Override
            public String getFileSuffix() {
                return "-nature";
            }
        };
    }
}
