package paths;

import java.util.List;
import java.util.function.Function;

import static paths.DirAB.A;

public class PriorityGenerator {
    public static Function<Integer, Double> getDijkstra(DirAB dir) {
        return SSSP.getNodeDist(dir)::get;
    }

    public static Function<Integer, Double> getAStar(DirAB dir) {
        List<Double> nodeDist = SSSP.getNodeDist(dir);
        return (i) -> nodeDist.get(i) + SSSP.getHeuristicFunction().apply(i, SSSP.getTarget());
    }

    public static Function<Integer, Double> getBiAStar(DirAB dir) {
        List<Double> nodeDist = SSSP.getNodeDist(dir);
        return (i) -> {
            HeuristicFunction heuristicFunction = SSSP.getHeuristicFunction();
            double pFunctionForward = (heuristicFunction.apply(i, SSSP.getTarget()) - heuristicFunction.apply(i, SSSP.getSource())) / 2;
            if (dir == A) {
                return nodeDist.get(i) + pFunctionForward;
            } else {
                return nodeDist.get(i) - pFunctionForward;
            }
        };
    }

    public static Function<Integer, Double> getBiAStarSymmetric(DirAB dir) {
        List<Double> nodeDist = SSSP.getNodeDist(dir);
        return (i) -> {
            HeuristicFunction heuristicFunction = SSSP.getHeuristicFunction();
            if (dir == A) {
                return nodeDist.get(i) + heuristicFunction.apply(i, SSSP.getTarget());
            } else {
                return nodeDist.get(i) + heuristicFunction.apply(i, SSSP.getSource());
            }
        };
    }
}
