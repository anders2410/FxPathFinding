package paths.generator;

import model.Edge;
import model.EdgeInfo;
import paths.SSSP;

import java.util.function.Function;

public class EdgeWeightGenerator {

    public static Function<Edge, Double> getDistanceWeights() {
        return (e) -> e.d;
    }

    public static Function<Edge, Double> getMaxSpeedTime() {
        return (e) -> {
            EdgeInfo info = SSSP.getGraphInfo().getEdge(e);
            return e.d / info.getMaxSpeed();
        };
    }

}
