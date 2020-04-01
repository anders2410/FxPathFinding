package paths.generator;

import paths.strategy.HeuristicFunction;
import paths.strategy.PriorityStrategy;

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

    public static PriorityStrategy getNonConHeuristic() {
        return (i, dir) -> {
            List<Double> nodeDist = getNodeDist(dir);
            HeuristicFunction heuristicFunction = getHeuristicFunction();
            if (dir == A) {
                return nodeDist.get(i) + ((heuristicFunction.apply(i, getTarget()) - heuristicFunction.apply(getSource(), i)) / 2) + heuristicFunction.apply(getSource(), getTarget()) / 2;
            } else {
                return nodeDist.get(i) + ((heuristicFunction.apply(getSource(), i) - heuristicFunction.apply(i, getTarget())) / 2) + heuristicFunction.apply(getSource(), getTarget()) / 2;
            }
        };
    }

    public static PriorityStrategy getConHeuristic() {
        return (i, dir) -> {
            List<Double> nodeDist = getNodeDist(dir);
            HeuristicFunction heuristicFunction = getHeuristicFunction();
            if (dir == A) {
                return nodeDist.get(i) + heuristicFunction.apply(i, getTarget());
            } else {
                return nodeDist.get(i) + heuristicFunction.apply(getSource(), i);
            }
        };
    }
}
