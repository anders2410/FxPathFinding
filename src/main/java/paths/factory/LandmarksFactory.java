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
        return HeuristicGenerator.landmarksTriangulate();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getDijkstra();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return (i) -> false;
    }

    @Override
    public PreprocessStrategy getPreprocessStrategy() {
        return new LandmarksPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getOneDirectional();
    }
}
