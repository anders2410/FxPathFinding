package paths.factory;

import paths.*;

import java.util.function.Function;

public class AStarFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public Function<Integer, Double> getPriorityStrategy(DirAB dir) {
        return PriorityGenerator.getAStar(dir);
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy(DirAB dir) {
        return RelaxGenerator.getAStar(dir);
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getConsistentStrategy();
    }
}
