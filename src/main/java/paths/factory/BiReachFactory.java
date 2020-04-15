package paths.factory;

import paths.strategy.*;

public class BiReachFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return null;
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return null;
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return null;
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return null;
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return null;
    }

    @Override
    public GetPQueueStrategy getPriorityQueue() {
        return null;
    }
}
