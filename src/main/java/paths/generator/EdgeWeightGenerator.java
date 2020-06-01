package paths.generator;

import model.Edge;
import info_model.EdgeInfo;
import model.Node;
import paths.ABDir;
import paths.SSSP;
import paths.strategy.EdgeWeightStrategy;

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
                EdgeInfo info = SSSP.getGraphInfo().getEdge(dir == A && !SSSP.reverseMe ? e : e.getReverse());
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
}
