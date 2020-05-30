package paths.strategy;

import paths.ABDir;

public interface QueueUpdatingStrategy {
    void updatePriority(Integer toUpdate, ABDir dir);
}
