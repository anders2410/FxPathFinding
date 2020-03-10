package paths;
import static paths.DirAB.*;

public class TerminationGenerator {

    public static TerminationStrategy getConsistentStrategy() {
        return (forwardNodeDist, forwardEstimatedNodeDist, forwardQueue, backwardNodeDist, backwardEstimatedNodeDist, backwardQueue, goal) -> {
            Integer topA = forwardQueue.peek();
            Integer topB = backwardQueue.peek();
            if (topA != null && topB != null) {
                return SSSP.getVisited(B).contains(topA) || SSSP.getVisited(A).contains(topB);
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
