package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class BiDijkstraFactory implements AlgorithmFactory {
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
        return RelaxGenerator.getBiDijkstraWithEdgePrune();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getKeyAboveGoalStrategy();
    }

    @Override
    public PreProcessStrategy getPreProcessStrategy() {
        return () -> {};
    }
    @Override
    public ScanPruningStrategy getScanPruningStrategy() {
        return ScanPruningGenerator.getBasePruning();
    }

    @Override
    public ResultPackingStrategy getResultPackingStrategy() {
        return ResultPackingGenerator.getStandardBiPack();

    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getAmountSeenStrategy();
    }
}
