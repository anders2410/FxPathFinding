package paths.generator;

import model.Edge;
import paths.ABDir;
import paths.strategy.RelaxStrategy;

import static paths.ABDir.A;
import static paths.ABDir.B;
import static paths.SSSP.*;

public class RelaxGenerator {

    public static RelaxStrategy getDijkstra() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edge.d;

            if (newDist < getNodeDist(dir).get(edge.to)) {
                getNodeDist(dir).set(edge.to, newDist);
                updatePriority(edge.to, dir);
                getPathMap(dir).put(edge.to, from);
/*
                trace(getQueue(dir), dir);
*/
            }
        };
    }
/*
    public static RelaxStrategy getAStarNew() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edge.d;
            double heuristicFrom = getHeuristicFunction().apply(from, getTarget());
            double heuristicNeighbour = getHeuristicFunction().apply(edge.to, getTarget());
            double weirdWeight = getNodeDist(dir).get(from) + edge.d - heuristicFrom + heuristicNeighbour;
            if (weirdWeight < getNodeDist(dir).get(edge.to)) {
                getNodeDist(dir).set(edge.to, newDist);
                getEstimatedDist(dir).put(edge.to, weirdWeight);
                getPathMap(dir).put(edge.to, from);
                getQueue(dir).updatePriority(edge.to);
            }
        };
    }

    public static RelaxStrategy getAStar() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edge.d;
            double heuristicFrom = getHeuristicFunction().apply(from, getTarget());
            double heuristicNeighbour = getHeuristicFunction().apply(edge.to, getTarget());
            double weirdWeight = getEstimatedDist(dir).get(from) + edge.d - heuristicFrom + heuristicNeighbour;
            updateNode(dir, from, edge, newDist, weirdWeight);
        };
    }

    public static RelaxStrategy getSymmetric() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edge.d;
            double newEst = getEstimatedDist(dir).get(from) + edge.d;
            double potentialFunc;
            if (dir == A) {
                potentialFunc = getHeuristicFunction().apply(edge.to, getTarget()) - getHeuristicFunction().apply(from, getTarget());
            } else {
                potentialFunc = getHeuristicFunction().apply(edge.to, getSource()) - getHeuristicFunction().apply(from, getSource());
            }
            double weirdWeight = newEst + potentialFunc;
            updateNode(dir, from, edge, newDist, weirdWeight);
        };
    }

    public static RelaxStrategy getConsistent() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edge.d;
            double newEst = getEstimatedDist(dir).getOrDefault(from, Double.MAX_VALUE) + edge.d;
            double pForwardFrom = (getHeuristicFunction().apply(from, getTarget()) - getHeuristicFunction().apply(from, getSource())) / 2;
            double pForwardTo = (getHeuristicFunction().apply(edge.to, getTarget()) - getHeuristicFunction().apply(edge.to, getSource())) / 2;
            double pBackwardFrom = (getHeuristicFunction().apply(from, getSource()) - getHeuristicFunction().apply(from, getTarget())) / 2;
            double pBackwardTo = (getHeuristicFunction().apply(edge.to, getSource()) - getHeuristicFunction().apply(edge.to, getTarget())) / 2;
            assert edge.d - pForwardFrom + pForwardTo == edge.d - -pForwardTo + -pForwardFrom;
            double pFunc = -pForwardFrom + pForwardTo;
            if (dir == B) {
                pFunc = -pBackwardFrom + pBackwardTo;
            }
            assert pForwardFrom + pBackwardFrom == pForwardTo + pBackwardTo;

            // double potentialFuncStart = -distanceStrategy.apply(nodeList.get(from), nodeList.get(source)) + distanceStrategy.apply(nodeList.get(edge.to), nodeList.get(source));
            assert pFunc + edge.d >= 0;
            double estimatedWeight = newEst + pFunc;
            updateNode(dir, from, edge, newDist, estimatedWeight);
        };
    }

    private static void updateNode(ABDir dir, int from, Edge edge, double newDist, double weirdWeight) {
        if (weirdWeight < getEstimatedDist(dir).getOrDefault(edge.to, Double.MAX_VALUE)) {
            getEstimatedDist(dir).put(edge.to, weirdWeight);
            getNodeDist(dir).set(edge.to, newDist);
            getPathMap(dir).put(edge.to, from);
            getQueue(dir).updatePriority(edge.to);
        }
    }*/
}
