package paths.generator;

import paths.ABDir;
import paths.SSSP;
import paths.ShortestPathResult;
import paths.strategy.ResultPackingStrategy;
import paths.strategy.ScanPruningStrategy;

import static paths.SSSP.*;

public class ResultPackingGenerator {
    public static ResultPackingStrategy getOneDirectionalPack() {
        return (shortestPath, duration) -> {
            return new ShortestPathResult(getNodeDist(ABDir.A).get(getTarget()), shortestPath, getScanned(ABDir.A), getRelaxed(ABDir.A), duration);
        };
    }

    public static ResultPackingStrategy getSingleToAllPack() {
        return (shortestPath, duration) -> {
            return new ShortestPathResult(0, shortestPath, getScanned(ABDir.A), getRelaxed(ABDir.A), getNodeDist(ABDir.A), getPathMap(ABDir.A), duration);
        };
    }

}
