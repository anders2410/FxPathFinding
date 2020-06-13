package paths.generator;

import paths.SSSP;
import paths.strategy.ScanPruningStrategy;

import static paths.SSSP.*;

public class ScanPruningGenerator {
    public static ScanPruningStrategy getBasePruning() {
        // return (dir, nodeToScan) -> getStalled().get(nodeToScan);
        return (dir, nodeToScan) -> nodeToScan == null;
    }

    public static ScanPruningStrategy getDubPruning() {
        return (dir, nodeToScan) -> {
            if (getScanned(dir).contains(nodeToScan)) return true;
            return nodeToScan == null;
        };
    }

    public static ScanPruningStrategy getCHDubPruning() {
        return (dir, nodeToScan) -> {
            if (getDubPruning().checkPrune(dir, nodeToScan)) return true;
            return getNodeDist(dir).get(nodeToScan) > getBestPathLengthSoFar();
        };
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
