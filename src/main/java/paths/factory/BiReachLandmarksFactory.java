package paths.factory;

import paths.generator.PreProcessGenerator;
import paths.strategy.*;

public class BiReachLandmarksFactory implements AlgorithmFactory {
    @Override
    public boolean isBiDirectional() {
        return true;
    }

    @Override
    public PriorityStrategy getPriorityStrategy() {
        return null;
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return null;
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return null;
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return null;
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return PreProcessGenerator.getRealPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return null;
    }
}
