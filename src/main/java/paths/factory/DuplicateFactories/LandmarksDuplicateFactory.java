package paths.factory.DuplicateFactories;

import paths.factory.LandmarksFactory;
import paths.generator.GetPQueueGenerator;
import paths.generator.QueueUpdateGenerator;
import paths.strategy.GetPQueueStrategy;
import paths.strategy.QueueUpdatingStrategy;

public class LandmarksDuplicateFactory extends LandmarksFactory {
    @Override
    public QueueUpdatingStrategy getQueueUpdatingStrategy() {
        return QueueUpdateGenerator.getDuplicateStrategy();
    }

    @Override
    public GetPQueueStrategy getQueue() {
        return GetPQueueGenerator.getDuplicateQueue();
    }
}
