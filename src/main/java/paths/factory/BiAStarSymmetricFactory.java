package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class BiAStarSymmetricFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getConHeuristic();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getDijkstra();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSymmetricStrategy();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return () -> {
        };
    }

    @Override
    public GetPQueueStrategy getPriorityQueue() {
        return GetPQueueGenerator.getJavaQueue();
    }
}
