package paths.generator;

import paths.SSSP;
import paths.strategy.ScanPruningStrategy;

import static paths.SSSP.getBestPathLengthSoFar;
import static paths.SSSP.getNodeDist;

public class ScanPruningGenerator {
    public static ScanPruningStrategy getBasePruning() {
        return (dir, nodeToScan) -> nodeToScan == null;
    }

    public static ScanPruningStrategy getBoundsPruning() {
        return (dir, nodeToScan) -> {
            if (getBasePruning().checkPrune(dir, nodeToScan)) return true;
            return getNodeDist(dir).get(nodeToScan) > SSSP.getSingleToAllBound();
        };
    }

    public static ScanPruningStrategy getCHPruning() {
        return (dir, nodeToScan) -> {
            if (getBasePruning().checkPrune(dir, nodeToScan)) return true;
            return getNodeDist(dir).get(nodeToScan) > getBestPathLengthSoFar();
        };
    }

}
