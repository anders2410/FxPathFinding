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
        return PriorityGenerator.getNonConHeuristic();
    }

    @Override
    public HeuristicFunction getHeuristicFunction() {
        return HeuristicGenerator.landmarksTriangulate();
    }

    @Override
    public RelaxStrategy getRelaxStrategy() {
        return RelaxGenerator.getBiDijkstra();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSearchMeetTermination();
    }
    @Override
    public ScanPruningStrategy getScanPruningStrategy() {
        return ScanPruningGenerator.getBasePruning();
    }
    @Override
    public PreProcessStrategy getPreProcessStrategy() {
        return PreProcessGenerator.getLandmarksPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getAmountSeenStrategy();
    }
}
