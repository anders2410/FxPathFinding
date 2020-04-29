package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class OneToAllDijkstra implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getDijkstra();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getDistance();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getDijkstra();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getEmptyStoppingStrategy();
    }

    @Override
    public PreProcessStrategy getPreProcessStrategy() {
        return () -> {};
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getOneDirectional();
    }
}
