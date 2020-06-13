package paths.factory.DuplicateFactories;

import paths.factory.ContractionHierarchiesFactory;
import paths.generator.GetPQueueGenerator;
import paths.generator.QueueUpdateGenerator;
import paths.generator.ScanPruningGenerator;
import paths.strategy.GetPQueueStrategy;
import paths.strategy.QueueUpdatingStrategy;
import paths.strategy.ScanPruningStrategy;

public class ContractionHierarchiesDuplicateFactory extends ContractionHierarchiesFactory {
    @Override
    public ScanPruningStrategy getScanPruningStrategy() {
        return ScanPruningGenerator.getCHDubPruning();
    }

    @Override
    public QueueUpdatingStrategy getQueueUpdatingStrategy() {
        return QueueUpdateGenerator.getDuplicateStrategy();
    }

    @Override
    public GetPQueueStrategy getQueue() {
        return GetPQueueGenerator.getDuplicateQueue();
    }
}
