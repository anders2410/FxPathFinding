package paths.factory;

import paths.generator.HeuristicGenerator;
import paths.generator.PriorityGenerator;
import paths.generator.RelaxGenerator;
import paths.generator.TerminationGenerator;
import paths.strategy.HeuristicFunction;
import paths.strategy.PriorityStrategy;
import paths.strategy.RelaxStrategy;
import paths.strategy.TerminationStrategy;

public class BiAStarConsistentFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getBiAStar();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getConsistent();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getConsistentStrategy();
    }
}
