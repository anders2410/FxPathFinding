package paths.strategy;

import paths.ABDir;

public interface QueuePollingStrategy {
    Integer extractMinFromQueue(ABDir dir);
}
