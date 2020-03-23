package paths.factory;

import paths.generator.HeuristicGenerator;
import paths.generator.PriorityGenerator;
import paths.generator.RelaxGenerator;
import paths.generator.TerminationGenerator;
import paths.strategy.*;

public class BiAStarMakeConsistentFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getNonConHeuristic();
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
        return TerminationGenerator.strongNonConHeuristicTermination();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return () -> {
        };
    }
}
