package paths.generator;
import paths.strategy.HeuristicFunction;
import paths.SSSP;
import paths.strategy.TerminationStrategy;

import static paths.ABDir.*;
import static paths.SSSP.*;

public class TerminationGenerator {

    public static TerminationStrategy getConsistentStrategy() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                return getVisited(B).contains(topA) || getVisited(A).contains(topB);
            }
            return false;
        };
    }

    public static TerminationStrategy getSymmetricStrategy(HeuristicFunction heuristicFunction) {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                double keyValueForward = getNodeDist(A).get(topA) + heuristicFunction.apply(topA, SSSP.getTarget());
                double keyValueBackwards = getNodeDist(B).get(topB) + heuristicFunction.apply(topB, SSSP.getSource());
                return keyValueBackwards >= goalDist || keyValueForward >= goalDist;
            }
            return false;
        };
    }
}
