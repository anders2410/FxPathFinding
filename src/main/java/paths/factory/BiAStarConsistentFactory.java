package paths.factory;

import paths.*;

import java.util.function.Function;

public class BiAStarConsistentFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public Function<Integer, Double> getPriorityStrategy(DirAB dir) {
        return PriorityGenerator.getBiAStar(dir);
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy(DirAB dir) {
        return RelaxGenerator.getConsistent(dir);
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getConsistentStrategy();
    }
}
