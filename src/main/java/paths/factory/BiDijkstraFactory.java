package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class BiDijkstraFactory implements AlgorithmFactory {
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
        return RelaxGenerator.getBiDijkstra();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getKeyOverGoalStrategy();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return () -> {};
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getAmountSeenStrategy();
    }
}
