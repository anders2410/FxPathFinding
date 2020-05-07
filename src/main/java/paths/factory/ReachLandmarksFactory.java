package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class ReachLandmarksFactory implements AlgorithmFactory {

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
        return RelaxGenerator.getReach();
    }

    @Override
    public ScanPruningStrategy getScanPruningStrategy() {
        return ScanPruningGenerator.getBasePruning();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSearchMeetTermination();
    }

    @Override
    public PreProcessStrategy getPreProcessStrategy() {
        return PreProcessGenerator.getRealPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getOneDirectional();
    }
}
