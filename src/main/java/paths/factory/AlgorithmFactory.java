package paths.factory;

import paths.strategy.*;

public interface AlgorithmFactory {
    boolean isBiDirectional();

    PriorityStrategy getPriorityStrategy();

    HeuristicFunction getHeuristicFunction();

    RelaxStrategy getRelaxStrategy();

    TerminationStrategy getTerminationStrategy();

    PreprocessStrategy getPreprocessStrategy();

    GetPQueueStrategy getPriorityQueue();
}
