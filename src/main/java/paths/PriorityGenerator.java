package paths;

import java.util.List;

import static paths.ABDir.A;
import static paths.SSSP.*;

public class PriorityGenerator {
    public static PriorityStrategy getDijkstra() {
        return (i, dir) -> getNodeDist(dir).get(i);
    }

    public static PriorityStrategy getAStar() {
        return (i, dir) -> getNodeDist(dir).get(i) + getHeuristicFunction().apply(i, getTarget());
    }

    public static PriorityStrategy getBiAStar() {
        return (i, dir) -> {
            List<Double> nodeDist = getNodeDist(dir);
            HeuristicFunction heuristicFunction = getHeuristicFunction();
            double pFunctionForward = (heuristicFunction.apply(i, getTarget()) - heuristicFunction.apply(i, getSource())) / 2;
            if (dir == A) {
                return nodeDist.get(i) + pFunctionForward;
            } else {
                return nodeDist.get(i) - pFunctionForward;
            }
        };
    }

    public static PriorityStrategy getBiAStarSymmetric() {
        return (i, dir) -> {
            List<Double> nodeDist = getNodeDist(dir);
            HeuristicFunction heuristicFunction = getHeuristicFunction();
            if (dir == A) {
                return nodeDist.get(i) + heuristicFunction.apply(i, getTarget());
            } else {
                return nodeDist.get(i) + heuristicFunction.apply(i, getSource());
            }
        };
    }
}
