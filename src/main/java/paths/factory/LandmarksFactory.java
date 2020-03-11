package paths.factory;

import paths.generators.HeuristicGenerator;
import paths.generators.PriorityGenerator;
import paths.generators.RelaxGenerator;
import paths.generators.TerminationGenerator;
import paths.strategy.HeuristicFunction;
import paths.strategy.PriorityStrategy;
import paths.strategy.RelaxStrategy;
import paths.strategy.TerminationStrategy;

public class LandmarksFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getAStar();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getLandmarks();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getAStar();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getConsistentStrategy();
    }
}
