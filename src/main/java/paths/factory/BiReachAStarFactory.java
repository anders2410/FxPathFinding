package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class BiReachAStarFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getAStar();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getBiReachAStar();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getKeyAboveGoalStrategy();
    }

    @Override
    public PreProcessStrategy getPreProcessStrategy() {
        return PreProcessGenerator.getReachPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getAmountSeenStrategy();
    }
}
