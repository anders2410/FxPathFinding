package paths.factory;

import paths.*;

import java.util.function.Function;

public class BiAStarSymmetricFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public Function<Integer, Double> getPriorityStrategy(boolean isForward) {
        return PriorityGenerator.getBiAStarSymmetric(isForward);
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return null;
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSymmetricStrategy(getHeuristicFunction());
    }
}
