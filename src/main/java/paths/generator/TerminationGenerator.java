package paths.generator;

import paths.strategy.TerminationStrategy;

import static paths.ABDir.A;
import static paths.ABDir.B;
import static paths.SSSP.*;

public class TerminationGenerator {
    public static TerminationStrategy strongNonConHeuristicTermination() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                double forwardKeyVal = getPriorityFunction().apply(topA, A);
                double backwardKeyVal = getPriorityFunction().apply(topB, B);
                double pForwardSource = ((getHeuristicFunction().apply(getSource(), getTarget()) - getHeuristicFunction().apply(getSource(), getSource())) / 2) + getHeuristicFunction().apply(getSource(), getTarget()) / 2;
                if (forwardKeyVal + backwardKeyVal >= goalDist + pForwardSource) {
                    return forwardKeyVal + backwardKeyVal >= goalDist + pForwardSource;
                }
            }
            return false;
        };
    }

    public static TerminationStrategy getSearchMeetTermination() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                return getVisited(B).contains(topA) || getVisited(A).contains(topB);
            }
            return false;
        };
    }

    public static TerminationStrategy getKeyOverGoalStrategy() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                double keyValueForward = getPriorityFunction().apply(topA, A);
                double keyValueBackwards = getPriorityFunction().apply(topB, B);
                return keyValueBackwards + keyValueForward > goalDist; //Changed to be strictly larger than
            }
            return false;
        };
    }

    public static TerminationStrategy getSymmetricStrategy() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                double keyValueForward = getPriorityFunction().apply(topA, A);
                double keyValueBackwards = getPriorityFunction().apply(topB, B);
                return keyValueBackwards >= goalDist || keyValueForward >= goalDist;
            }
            return false;
        };
    }

    public static TerminationStrategy getEmptyStoppingStrategy() {
        return (goalDist) -> false;
    }

    public static TerminationStrategy strongConHeuristicTermination() {
        return (goalDist) -> {
            Integer topA = getQueue(A).peek();
            Integer topB = getQueue(B).peek();
            if (topA != null && topB != null) {
                double forwardKeyVal = getPriorityFunction().apply(topA, A);
                double backwardKeyVal = getPriorityFunction().apply(topB, B);
                double pForwardSource = getHeuristicFunction().apply(getSource(), getTarget());
                if (forwardKeyVal + backwardKeyVal >= goalDist) {
                    return forwardKeyVal + backwardKeyVal >= goalDist;
                }
            }
            return false;
        };
    }


}
