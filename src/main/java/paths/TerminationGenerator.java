package paths;

import model.Node;

import java.util.List;

public class TerminationGenerator {

    public static TerminationStrategy getConsistentStrategy() {
        return (forwardNodeDist, forwardEstimatedNodeDist, forwardQueue, backwardNodeDist, backwardEstimatedNodeDist, backwardQueue, goal) -> {
            Integer topA = forwardQueue.peek();
            Integer topB = backwardQueue.peek();
            if (topA != null && topB != null) {
                return SSSP.getVisitedB().contains(topA) || SSSP.getVisitedA().contains(topB);
            }
            return false;
        };
    }

    public static TerminationStrategy getSymmetricStrategy(HeuristicFunction heuristicFunction) {
        return (forwardNodeDist, forwardEstimatedNodeDist, forwardQueue, backwardNodeDist, backwardEstimatedNodeDist, backwardQueue, goal) -> {
            Integer topA = forwardQueue.peek();
            Integer topB = backwardQueue.peek();
            if (topA != null && topB != null) {
                double keyValueForward = forwardNodeDist.get(topA) + heuristicFunction.apply(topA, SSSP.getTarget());
                double keyValueBackwards = backwardNodeDist.get(topB) + heuristicFunction.apply(topB, SSSP.getSource());
                return keyValueBackwards >= goal || keyValueForward >= goal;
            }
            return false;
        };
    }
}
