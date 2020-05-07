package paths.strategy;

import paths.ABDir;

public interface ScanPruningStrategy {
    boolean checkPrune(ABDir dir, Integer currentNode);
}
