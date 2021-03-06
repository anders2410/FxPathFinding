package paths.generator;

import paths.SSSP;
import paths.strategy.TerminationStrategy;

import static paths.ABDir.A;
import static paths.ABDir.B;
import static paths.SSSP.*;

public class TerminationGenerator {
    public static TerminationStrategy getStrongNonConHeuristicTermination() {
        return (goalDist) -> {
            Integer topA = getQueue(A).nodePeek();
            Integer topB = getQueue(B).nodePeek();
            if (topA != null && topB != null) {
                double forwardKeyVal = getPriorityStrategy().apply(topA, A);
                double backwardKeyVal = getPriorityStrategy().apply(topB, B);
                double pForwardSource = ((getHeuristicFunction().apply(getSource(), getTarget()) - getHeuristicFunction().apply(getSource(), getSource())) / 2) + getHeuristicFunction().apply(getSource(), getTarget()) / 2;
                if (forwardKeyVal + backwardKeyVal >= goalDist + pForwardSource) {
                    return forwardKeyVal + backwardKeyVal >= goalDist + pForwardSource;
                }
            }
            return false;
        };
    }


    public static TerminationStrategy getSameScannedTermination() {
        return (goalDist) -> {
            Integer topA = getQueue(A).nodePeek();
            Integer topB = getQueue(B).nodePeek();
            if (topA != null && topB != null) {
                return getScanned(B).contains(topA) || getScanned(A).contains(topB);
            }
            return false;
        };
    }

    public static TerminationStrategy getBiReachTermination() {
        return (goalDist) -> {
            Integer topA = getQueue(A).nodePeek();
            Integer topB = getQueue(B).nodePeek();
            if (topA == null && topB == null) return false;
            if (topA == null) {
                SSSP.setAlternationStrategy(AlternationGenerator.getReverseOneDirectional());
                return false;
            } else if (topB == null) {
                SSSP.setAlternationStrategy(AlternationGenerator.getOneDirectional());
                return false;
            } else {
                double forwardKeyVal = getPriorityStrategy().apply(topA, A);
                double backwardKeyVal = getPriorityStrategy().apply(topB, B);
                boolean backwardsShouldStop = backwardKeyVal > goalDist / 2;
                if (backwardsShouldStop)
                    SSSP.setAlternationStrategy(AlternationGenerator.getOneDirectional());
                boolean forwardShouldStop = forwardKeyVal > goalDist / 2;
                if (forwardShouldStop)
                    SSSP.setAlternationStrategy(AlternationGenerator.getReverseOneDirectional());
                return backwardsShouldStop && forwardShouldStop;
            }
        };
    }

    public static TerminationStrategy getKeyAboveGoalStrategy() {
        return (goalDist) -> {
            Integer topA = getQueue(A).nodePeek();
            Integer topB = getQueue(B).nodePeek();
            if (topA != null && topB != null) {
                double keyValueForward = getPriorityStrategy().apply(topA, A);
                double keyValueBackwards = getPriorityStrategy().apply(topB, B);
                return keyValueBackwards + keyValueForward > goalDist;
            }
            return false;
        };
    }

    public static TerminationStrategy getSymmetricStrategy() {
        return (goalDist) -> {
            Integer topA = getQueue(A).nodePeek();
            Integer topB = getQueue(B).nodePeek();
            if (topA != null && topB != null) {
                double keyValueForward = getPriorityStrategy().apply(topA, A);
                double keyValueBackwards = getPriorityStrategy().apply(topB, B);
                return keyValueBackwards >= goalDist || keyValueForward >= goalDist;
            }
            return false;
        };
    }

    public static TerminationStrategy getEmptyStoppingStrategy() {
        return (goalDist) -> false;
    }

    public static TerminationStrategy getBoundedStoppingStrategy() {
        return goalDistance -> !(getNodeDist(A).get(getQueue(A).nodePeek()) < SSSP.getSingleToAllBound());
    }

    public static TerminationStrategy getStrongConHeuristicTermination() {
        return (goalDist) -> {
            Integer topA = getQueue(A).nodePeek();
            Integer topB = getQueue(B).nodePeek();
            if (topA != null && topB != null) {
                double forwardKeyVal = getPriorityStrategy().apply(topA, A);
                double backwardKeyVal = getPriorityStrategy().apply(topB, B);
                double pForwardSource = getHeuristicFunction().apply(getSource(), getTarget());
                if (forwardKeyVal + backwardKeyVal >= goalDist) {
                    return forwardKeyVal + backwardKeyVal >= goalDist;
                }
            }
            return false;
        };
    }
}
