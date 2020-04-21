package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class ContractionHierarchiesFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
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
        return RelaxGenerator.getCH();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getEmptyStoppingStrategy();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return new ContractionHierarchyPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getAmountSeenStrategy();
    }
}
