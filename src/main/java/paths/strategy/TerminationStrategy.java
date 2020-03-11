package paths.strategy;

import paths.ABDir;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public interface TerminationStrategy {
    boolean checkTermination(double goalDistance);
}
