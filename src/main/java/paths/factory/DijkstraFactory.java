package paths.factory;

import paths.generator.HeuristicGenerator;
import paths.generator.PriorityGenerator;
import paths.generator.RelaxGenerator;
import paths.generator.TerminationGenerator;
import paths.strategy.*;

public class DijkstraFactory implements AlgorithmFactory {
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
        return TerminationGenerator.getConsistentStrategy();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return () -> {};
    }
}
