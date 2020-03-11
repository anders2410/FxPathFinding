package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class BiLandmarksFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getBiAStar();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getLandmarks();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getConsistent();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getConsistentStrategy();
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return new LandmarksPreStrategy();
    }
}
