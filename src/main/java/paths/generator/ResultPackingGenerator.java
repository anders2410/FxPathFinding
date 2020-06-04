package paths.generator;

import javafx.util.Pair;
import model.Edge;
import paths.ABDir;
import paths.SSSP;
import paths.ShortestPathResult;
import paths.strategy.ResultPackingStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static paths.SSSP.*;

public class ResultPackingGenerator {
    public static ResultPackingStrategy getOneDirectionalPack() {
        return (duration) -> {
            List<Integer> shortestPath = extractPath(getPathMap(ABDir.A), getSource(), getTarget());
            return new ShortestPathResult(getNodeDist(ABDir.A).get(getTarget()), shortestPath, getScanned(ABDir.A), getRelaxed(ABDir.A), duration);
        };
    }

    public static ResultPackingStrategy getSingleToAllPack() {
        return (duration) -> {
            List<Integer> shortestPath = new ArrayList<>();
            return new ShortestPathResult(0, shortestPath, getScanned(ABDir.A), getRelaxed(ABDir.A), getNodeDist(ABDir.A), getPathMap(ABDir.A), duration);
        };
    }

    public static ResultPackingStrategy getStandardBiPack() {
        return (duration) -> {
            if (SSSP.getMiddlePoint() == -1) return new ShortestPathResult();
            List<Integer> shortestPath = SSSP.extractPathBi();
            return new ShortestPathResult(getGoalDistance(), shortestPath, getScanned(ABDir.A), getScanned(ABDir.B), getRelaxed(ABDir.A), getRelaxed(ABDir.B), duration);
        };
    }

    public static ResultPackingStrategy getCHPack() {
        return (duration) -> {
            // Goes through all overlapping nodes and find the one with the smallest distance.
            int middlepoint = -1;
            double finalDistance = Double.MAX_VALUE;
            for (int node : getScanned(ABDir.A)) {
                if (getScanned(ABDir.A).contains(node) && getScanned(ABDir.B).contains(node)) {
                    // Replace if lower than actual
                    double distance = getNodeDist(ABDir.A).get(node) + getNodeDist(ABDir.B).get(node);
                    if (0 <= distance && distance < finalDistance) {
                        finalDistance = distance;
                        middlepoint = node;
                    }
                }
            }
            setMiddlePoint(middlepoint);
            // This is the unpacking of the shortest path. It unpacks all shortcuts on the route found by
            // the modified Bi-Directional Dijkstras.
            List<Integer> shortestPathCH = extractPathBi();
            List<Integer> result = new ArrayList<>(shortestPathCH);
            boolean shouldContinue = true;
            while (shouldContinue) {
                for (int i = 0; i < result.size() - 1; i++) {
                    Integer contractedNode = getCHResult().getShortcuts().get(new Pair<>(result.get(i), result.get(i + 1)));
                    if (contractedNode != null) {
                        result.add(i + 1, contractedNode);
                        break;
                    }
                }
                shouldContinue = shouldBeDone(result);
            }

            // This is used to unpack all the routes visited by CH;
            if (false) {
                Set<Integer> visitedA;
                Set<Integer> visitedB;

                Set<Integer> visitedNodeSet = new HashSet<>();
                for (int node : getScanned(ABDir.A)) {
                    if (getScanned(ABDir.A).contains(node) && getScanned(ABDir.B).contains(node)) {
                        visitedNodeSet.add(node);
                    }
                }

                visitedA = findVisitedNodes(visitedNodeSet, getSource(), ABDir.A);
                visitedB = findVisitedNodes(visitedNodeSet, getTarget(), ABDir.B);

                // Set<Integer> savedScannedA = new HashSet<>(getScanned(ABDir.A));
                // Set<Integer> savedScannedB = new HashSet<>(getScanned(ABDir.B));

                setScanned(ABDir.A, visitedA);
                setScanned(ABDir.B, visitedB);
            }

            return new ShortestPathResult(getGoalDistance(), new ArrayList<>(result), getScanned(ABDir.A), getScanned(ABDir.B), getRelaxed(ABDir.A), getRelaxed(ABDir.B), duration);
        };
    }

    private static Set<Integer> findVisitedNodes(Set<Integer> visitedNodeSet, int source, ABDir dir) {
        Set<Integer> temp = new HashSet<>();
        for (Integer i : visitedNodeSet) {
            List<Integer> tempShortestPath = extractPath(SSSP.getPathMap(dir), source, i);
            List<Integer> tempA = new ArrayList<>(tempShortestPath);
            boolean shouldContinueA = true;
            while (shouldContinueA) {
                for (int j = 0; j < tempA.size() - 1; j++) {
                    Integer contractedNode = getCHResult().getShortcuts().get(new Pair<>(tempA.get(j), tempA.get(j + 1)));
                    if (contractedNode != null) {
                        tempA.add(j + 1, contractedNode);
                        break;
                    }
                }
                shouldContinueA = shouldBeDone(tempA);
            }
            temp.addAll(tempA);
        }
        return temp;
    }

    private static boolean shouldBeDone(List<Integer> complete) {
        for (int i = 0; i < complete.size() - 1; i++) {
            Integer contractedNode = SSSP.getCHResult().getShortcuts().get(new Pair<>(complete.get(i), complete.get(i + 1)));
            if (contractedNode != null) {
                return true;
            }
        }

        return false;
    }
}
