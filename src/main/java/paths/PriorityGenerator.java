package paths;

import java.util.List;
import java.util.function.Function;

public class PriorityGenerator {
    public static Function<Integer, Double> getDijkstra(boolean isForward) {
        return SSSP.getNodeDist(isForward)::get;
    }

    public static Function<Integer, Double> getAStar(boolean isForward) {
        List<Double> nodeDist = SSSP.getNodeDist(isForward);
        return (i) -> nodeDist.get(i) + SSSP.getHeuristicFunction().apply(i, SSSP.getTarget());
    }

    public static Function<Integer, Double> getBiAStar(boolean isForward) {
        List<Double> nodeDist = SSSP.getNodeDist(isForward);
        return (i) -> {
            HeuristicFunction heuristicFunction = SSSP.getHeuristicFunction();
            double pFunctionForward = (heuristicFunction.apply(i, SSSP.getTarget()) - heuristicFunction.apply(i, SSSP.getSource())) / 2;
            if (isForward) {
                return nodeDist.get(i) + pFunctionForward;
            } else {
                return nodeDist.get(i) - pFunctionForward;
            }
        };
    }

    public static Function<Integer, Double> getBiAStarSymmetric(boolean isForward) {
        List<Double> nodeDist = SSSP.getNodeDist(isForward);
        return (i) -> {
            HeuristicFunction heuristicFunction = SSSP.getHeuristicFunction();
            if (isForward) {
                return nodeDist.get(i) + heuristicFunction.apply(i, SSSP.getTarget());
            } else {
                return nodeDist.get(i) + heuristicFunction.apply(i, SSSP.getSource());
            }
        };
    }
}
