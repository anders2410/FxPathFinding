package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class BiReachFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
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
        return TerminationGenerator.getKeyOverGoalStrategy();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return new ReachPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getSameDistanceStrategy();
    }
}
