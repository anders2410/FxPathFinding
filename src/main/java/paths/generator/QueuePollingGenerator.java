package paths.generator;

import paths.strategy.QueuePollingStrategy;

import static paths.SSSP.getQueue;
import static paths.SSSP.getScanned;

public class QueuePollingGenerator {
    public QueuePollingStrategy getRegularPolling() {
        return (dir) -> {
            return getQueue(dir).nodePoll();
        };
    }

    public QueuePollingStrategy getDuplicateQueuePolling() {
        return dir -> {
            boolean newEntryFound = false;
            Integer newNode = null;
            while (!newEntryFound) {
                newNode = getQueue(dir).nodePoll();
                if (newNode == null) return null;
                if (!getScanned(dir).contains(newNode)) newEntryFound = true;
            }
            return newNode;
        };
    }
}
