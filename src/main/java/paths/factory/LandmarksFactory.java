package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class LandmarksFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return false;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return PriorityGenerator.getAStar();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.getLandmarks();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getAStar();
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
