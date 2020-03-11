package paths.factory;

import paths.generator.HeuristicGenerator;
import paths.generator.PriorityGenerator;
import paths.generator.RelaxGenerator;
import paths.generator.TerminationGenerator;
import paths.strategy.*;

public class BiAStarSymmetricFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getBiAStarSymmetric();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getSymmetric();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSymmetricStrategy(getHeuristicFunction());
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return () -> {};
    }
}
