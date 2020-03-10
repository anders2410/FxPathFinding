package paths.factory;

import paths.*;

import java.util.function.Function;

public class DijkstraFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public Function<Integer, Double> getPriorityStrategy(DirAB dir) {
        return PriorityGenerator.getDijkstra(dir);
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy(DirAB dir) {
        return RelaxGenerator.getDijkstra(dir);
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getConsistentStrategy();
    }
}
