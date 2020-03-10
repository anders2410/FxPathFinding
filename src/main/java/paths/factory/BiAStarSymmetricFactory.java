package paths.factory;

import paths.*;

import java.util.function.Function;

public class BiAStarSymmetricFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public Function<Integer, Double> getPriorityStrategy(DirAB dir) {
        return PriorityGenerator.getBiAStarSymmetric(dir);
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy(DirAB dir) {
        return RelaxGenerator.getSymmetric(dir);
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSymmetricStrategy(getHeuristicFunction());
    }
}
