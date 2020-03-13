package paths.generator;

import paths.strategy.HeuristicFunction;
import paths.SSSP;
import paths.strategy.TerminationStrategy;
import paths.strategy.PriorityStrategy;

import java.util.HashSet;
import java.util.Set;

import static paths.ABDir.*;
import static paths.SSSP.*;
import static paths.generator.PriorityGenerator.getBiAStar;

public class TerminationGenerator {


    public static TerminationStrategy getStrongStoppingStrategy() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                return getPriorityFunction().apply(topA, A) + getPriorityFunction().apply(topB, B) > goalDist + ((getHeuristicFunction().apply(getTarget(), getSource()) - getHeuristicFunction().apply(getTarget(), getTarget())) / 2);
            }
            return false;
        };
    }

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

    public static TerminationStrategy getAdvancedSymmetricStrategy() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                double keyValueForward = getNodeDist(A).get(topA);
                double keyValueBackwards = getNodeDist(B).get(topB);
                return keyValueBackwards + keyValueForward >= goalDist;
            }
            return false;
        };
    }

    public static TerminationStrategy getSymmetricAStrategy(HeuristicFunction heuristicFunction) {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                double keyValueForward = getBiAStar().apply(topA, A);
                double keyValueBackwards = getBiAStar().apply(topB, B);
                return keyValueBackwards + keyValueForward >= goalDist + heuristicFunction.apply(getTarget(), getSource()) - heuristicFunction.apply(getTarget(), getTarget()) / 2;
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
