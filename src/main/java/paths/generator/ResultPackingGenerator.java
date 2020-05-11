package paths.generator;

import javafx.util.Pair;
import paths.ABDir;
import paths.SSSP;
import paths.ShortestPathResult;
import paths.strategy.ResultPackingStrategy;

import java.util.ArrayList;
import java.util.List;

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
            return new ShortestPathResult(getGoalDistance(), new ArrayList<>(result), getScanned(ABDir.A), getScanned(ABDir.B), getRelaxed(ABDir.A), getRelaxed(ABDir.B), duration);
        };
    }
}
