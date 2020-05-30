package paths.factory;

import paths.generator.GetPQueueGenerator;
import paths.generator.QueuePollingGenerator;
import paths.generator.QueueUpdateGenerator;
import paths.strategy.*;

public interface AlgorithmFactory {
    boolean isBiDirectional();

    PriorityStrategy getPriorityStrategy();

    HeuristicFunction getHeuristicFunction();

    RelaxStrategy getRelaxStrategy();

    TerminationStrategy getTerminationStrategy();

    PreProcessStrategy getPreProcessStrategy();

    AlternationStrategy getAlternationStrategy();

    ScanPruningStrategy getScanPruningStrategy();

    ResultPackingStrategy getResultPackingStrategy();

    default QueueUpdatingStrategy getQueueUpdatingStrategy() {
        return QueueUpdateGenerator.getRegularStrategy();
    }

    default GetPQueueStrategy getQueue() {
        return GetPQueueGenerator.getJavaQueue();
    }
}
