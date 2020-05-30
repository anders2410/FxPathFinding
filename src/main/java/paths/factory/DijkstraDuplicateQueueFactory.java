package paths.factory;

import paths.generator.GetPQueueGenerator;
import paths.generator.QueueUpdateGenerator;
import paths.strategy.GetPQueueStrategy;
import paths.strategy.QueueUpdatingStrategy;

public class DijkstraDuplicateQueueFactory extends DijkstraFactory {
    @Override
    public QueueUpdatingStrategy getQueueUpdatingStrategy() {
        return QueueUpdateGenerator.getDuplicateStrategy();
    }

    @Override
    public GetPQueueStrategy getQueue() {
        return GetPQueueGenerator.getDuplicateQueue();
    }
}
