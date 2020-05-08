package paths.strategy;

import paths.ABDir;
import paths.ShortestPathResult;

import java.util.List;

public interface ResultPackingStrategy {
    ShortestPathResult packResult(List<Integer> path, long duration);
}
