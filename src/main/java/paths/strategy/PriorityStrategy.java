package paths.strategy;

import paths.ABDir;

public interface PriorityStrategy {
    double apply(int node, ABDir dir);
}
