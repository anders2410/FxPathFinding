package paths.generator;

import javafx.util.Pair;
import model.Edge;
import model.Node;
import paths.ABDir;
import paths.SSSP;
import paths.Util;
import paths.strategy.EdgeWeightStrategy;
import paths.strategy.RelaxStrategy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static paths.ABDir.A;
import static paths.SSSP.*;
import static paths.Util.revDir;

public class RelaxGenerator {

    private static EdgeWeightStrategy edgeWeightStrategy = EdgeWeightGenerator.getDistanceWeights();

    public static RelaxStrategy getDijkstra() {
        return (edge, dir) -> {
            double newDist = getNodeDist(dir).get(edge.from) + edgeWeightStrategy.getWeight(edge, dir);
            if (newDist < getNodeDist(dir).get(edge.to)) {
                getNodeDist(dir).set(edge.to, newDist);
                updatePriority(edge.to, dir);
                getPathMap(dir).put(edge.to, edge.from);
                putRelaxedEdge(dir, edge);
            }
        };
    }

    public static RelaxStrategy getBiDijkstra() {
        return (edge, dir) -> {
            getDijkstra().relax(edge, dir);
            updateGoalDist(edge, dir);
        };
    }

    public static RelaxStrategy getBiDijkstraWithEdgePrune() {
        return (edge, dir) -> {
            if (!getScanned(revDir(dir)).contains(edge.to)) {
                getDijkstra().relax(edge, dir);
                updateGoalDist(edge, dir);
            }
        };
    }

    private static boolean updateGoalDist(Edge edge, ABDir dir) {
        double newPathDist = getNodeDist(dir).get(edge.from) + edgeWeightStrategy.getWeight(edge, dir) + getNodeDist(revDir(dir)).get(edge.to);
        if (newPathDist < getGoalDistance()) {
            setGoalDistance(newPathDist);
            setMiddlePoint(edge.to);
            return true;
        }
        return false;
    }

    public static RelaxStrategy getReach() {
        return (edge, dir) -> {
            if (reachValid(edge, dir)) {
                getDijkstra().relax(edge, dir);
            }
        };
    }

    public static RelaxStrategy getBiReachAStar() {
        return (edge, dir) -> {
            /*List<Double> bounds = getReachBounds();
            double reachBound = bounds.get(edge.to);
            boolean distBiggerThanReach = getNodeDist(dir).get(edge.from) > reachBound && !(Math.abs(reachBound - getNodeDist(dir).get(edge.from)) <= precision);
            double nodePotential;
            if (dir == ABDir.A) {
                nodePotential = ((getHeuristicFunction().apply(edge.from, getTarget()) - getHeuristicFunction().apply(getSource(), edge.from)) / 2) + getHeuristicFunction().apply(getSource(), getTarget()) / 2;
            } else {
                nodePotential = ((getHeuristicFunction().apply(getSource(), edge.from) - getHeuristicFunction().apply(edge.from, getTarget())) / 2) + getHeuristicFunction().apply(getSource(), getTarget()) / 2;
            }
            boolean potentialBiggerThanReach = nodePotential > reachBound && !(Math.abs(reachBound - nodePotential) <= precision);
            boolean newDistanceInValid = distBiggerThanReach && potentialBiggerThanReach;*/
            double newDist = getNodeDist(dir).get(edge.from);
            List<Double> bounds = getReachBounds();
            double reachBound = bounds.get(edge.from);
            double nodePotential;
            if (dir == A) {
                nodePotential = ((getHeuristicFunction().apply(edge.from, getTarget()) - getHeuristicFunction().apply(getSource(), edge.from)) / 2) + getHeuristicFunction().apply(getSource(), getTarget()) / 2;
            } else {
                nodePotential = ((getHeuristicFunction().apply(getSource(), edge.from) - getHeuristicFunction().apply(edge.from, getTarget())) / 2) + getHeuristicFunction().apply(getSource(), getTarget()) / 2;
            }
            boolean newDistanceValid = reachBound > newDist || Math.abs(reachBound - newDist) <= precision;
            boolean projectedDistanceValid = reachBound > nodePotential || Math.abs(reachBound - nodePotential) <= precision;
            boolean valid = newDistanceValid || projectedDistanceValid;
            if (valid) {
                getDijkstra().relax(edge, dir);
                updateGoalDist(edge, dir);
            }
        };
    }

    private static double precision = 0.00000000000001;

    private static boolean reachValid(Edge edge, ABDir dir) {
        double newDist = getNodeDist(dir).get(edge.from) + edgeWeightStrategy.getWeight(edge, dir);
        List<Double> bounds = getReachBounds();
        double reachBound = bounds.get(edge.to);
        List<Node> nodeList = getGraph().getNodeList();
        Double projectedDistance = getDistanceStrategy().apply(nodeList.get(edge.to), nodeList.get(getTarget()));
        boolean newDistanceValid = reachBound > newDist || Math.abs(reachBound - newDist) <= precision;
        boolean projectedDistanceValid = reachBound > projectedDistance || Math.abs(reachBound - projectedDistance) <= precision;
        return newDistanceValid || projectedDistanceValid;
    }

    // 13137 -> 550
    public static RelaxStrategy getBiReach() {
        return ((edge, dir) -> {
            List<Double> bounds = getReachBounds();
            double reachBound = bounds.get(edge.to);
            // Precision should not be checked here. Equality is okay.
            boolean newDistanceInValid = getNodeDist(dir).get(edge.from) > reachBound /*|| Math.abs(reachBound - newDist) <= precision*/;
            if (!newDistanceInValid) {
                getBiDijkstra().relax(edge, dir);
            } else {
                if (getScanned(Util.revDir(dir)).contains(edge.to)) {
                    int mp = getMiddlePoint();
                    updateGoalDist(edge, dir);
                    setMiddlePoint(mp);
                }
            }
        });
    }

    public static RelaxStrategy getCH() {
        return (edge, dir) -> {
            List<Integer> ranks = getCHResult().getRanks();
            //Map<Pair<Integer, Integer>, Integer> shortcuts = getCHResult().getShortcuts();
            if (ranks.get(edge.from) < ranks.get(edge.to)) {
                /*Pair<Integer, Integer> pair;
                if (dir == ABDir.A) {
                    pair = new Pair<>(edge.from, edge.to);
                } else {
                    pair = new Pair<>(edge.to, edge.from);
                }

                double edgeWeight = shortcuts.containsKey(pair) ? edge.d : edgeWeightStrategy.getWeight(edge, dir);
                double pathLength = getNodeDist(dir).get(edge.from) + edgeWeight + getNodeDist(revDir(dir)).get(edge.to);*/
                if (getScanned(revDir(dir)).contains(edge.to)) {
                    double pathLength = getNodeDist(dir).get(edge.from) + edgeWeightStrategy.getWeight(edge, dir) + getNodeDist(revDir(dir)).get(edge.to);
                    if (pathLength < getBestPathLengthSoFar()) {
                        setBestPathLengthSoFar(pathLength);
                    }
                }

                getDijkstra().relax(edge, dir);

                /*double newDist = getNodeDist(dir).get(edge.from) + edgeWeightStrategy.getWeight(edge, dir);
                if (newDist < getNodeDist(dir).get(edge.to)) {
                    getNodeDist(dir).set(edge.to, newDist);
                    updatePriority(edge.to, dir);
                    getPathMap(dir).put(edge.to, edge.from);
                    putRelaxedEdge(dir, edge);
                    //if (SSSP.getStalled().get(edge.from)) SSSP.getStalled().set(edge.from, false);
                }*/
            }
        };
    }

    public static RelaxStrategy getBoundedDijkstra() {
        return (edge, dir) -> {
            double newDist = getNodeDist(dir).get(edge.from) + edgeWeightStrategy.getWeight(edge, dir);
            /*if (getNodeDist(dir).get(edge.from) > SSSP.getSingleToAllBound()) return;*/
            if (newDist < getNodeDist(dir).get(edge.to)) {
                getNodeDist(dir).set(edge.to, newDist);
                updatePriority(edge.to, dir);
                getPathMap(dir).put(edge.to, edge.from);
                putRelaxedEdge(dir, edge);
            }
        };
    }

    public static void setEdgeWeightStrategy(EdgeWeightStrategy edgeWeightStrategy) {
        RelaxGenerator.edgeWeightStrategy = edgeWeightStrategy;
    }

    /*
    public static RelaxStrategy getAStarNew() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edgeWeightStrategy.apply(edge);
            double heuristicFrom = getHeuristicFunction().apply(from, getTarget());
            double heuristicNeighbour = getHeuristicFunction().apply(edge.to, getTarget());
            double weirdWeight = getNodeDist(dir).get(from) + edgeWeightStrategy.apply(edge) - heuristicFrom + heuristicNeighbour;
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
            double newDist = getNodeDist(dir).get(from) + edgeWeightStrategy.apply(edge);
            double heuristicFrom = getHeuristicFunction().apply(from, getTarget());
            double heuristicNeighbour = getHeuristicFunction().apply(edge.to, getTarget());
            double weirdWeight = getEstimatedDist(dir).get(from) + edgeWeightStrategy.apply(edge) - heuristicFrom + heuristicNeighbour;
            updateNode(dir, from, edge, newDist, weirdWeight);
        };
    }

    public static RelaxStrategy getSymmetric() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edgeWeightStrategy.apply(edge);
            double newEst = getEstimatedDist(dir).get(from) + edgeWeightStrategy.apply(edge);
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
            double newDist = getNodeDist(dir).get(from) + edgeWeightStrategy.apply(edge);
            double newEst = getEstimatedDist(dir).getOrDefault(from, Double.MAX_VALUE) + edgeWeightStrategy.apply(edge);
            double pForwardFrom = (getHeuristicFunction().apply(from, getTarget()) - getHeuristicFunction().apply(from, getSource())) / 2;
            double pForwardTo = (getHeuristicFunction().apply(edge.to, getTarget()) - getHeuristicFunction().apply(edge.to, getSource())) / 2;
            double pBackwardFrom = (getHeuristicFunction().apply(from, getSource()) - getHeuristicFunction().apply(from, getTarget())) / 2;
            double pBackwardTo = (getHeuristicFunction().apply(edge.to, getSource()) - getHeuristicFunction().apply(edge.to, getTarget())) / 2;
            assert edgeWeightStrategy.apply(edge) - pForwardFrom + pForwardTo == edgeWeightStrategy.apply(edge) - -pForwardTo + -pForwardFrom;
            double pFunc = -pForwardFrom + pForwardTo;
            if (dir == B) {
                pFunc = -pBackwardFrom + pBackwardTo;
            }
            assert pForwardFrom + pBackwardFrom == pForwardTo + pBackwardTo;

            // double potentialFuncStart = -distanceStrategy.apply(nodeList.get(from), nodeList.get(source)) + distanceStrategy.apply(nodeList.get(edge.to), nodeList.get(source));
            assert pFunc + edgeWeightStrategy.apply(edge) >= 0;
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
