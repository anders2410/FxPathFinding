package paths.factory;

import paths.ReachProcessor;
import paths.generator.*;
import paths.strategy.*;

public class ReachFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getDijkstra();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getReach();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSearchMeetTermination();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return new ReachPreStrategy();
    }

    @Override
    public GetPQueueStrategy getPriorityQueue() {
        return GetPQueueGenerator.getJavaQueue();
    }
}
