package paths.factory.DuplicateFactories;

import paths.factory.BiLandmarksFactory;
import paths.generator.GetPQueueGenerator;
import paths.generator.QueueUpdateGenerator;
import paths.strategy.GetPQueueStrategy;
import paths.strategy.QueueUpdatingStrategy;

public class BiLandmarksDuplicateFactory extends BiLandmarksFactory {
    @Override
    public QueueUpdatingStrategy getQueueUpdatingStrategy() {
        return QueueUpdateGenerator.getDuplicateStrategy();
    }

    @Override
    public GetPQueueStrategy getQueue() {
        return GetPQueueGenerator.getDuplicateQueue();
    }
}
