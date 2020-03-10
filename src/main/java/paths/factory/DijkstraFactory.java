package paths.factory;

import paths.*;

import java.util.function.Function;

public class DijkstraFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public Function<Integer, Double> getPriorityStrategy(boolean isForward) {
        return PriorityGenerator.getDijkstra(isForward);
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
        return TerminationGenerator.getConsistentStrategy();
    }
}
