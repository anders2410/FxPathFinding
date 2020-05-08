package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class BiReachFactory implements AlgorithmFactory {
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
        return RelaxGenerator.getBiReach();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getBiReachTermination();
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
    public PreProcessStrategy getPreProcessStrategy() {
        return PreProcessGenerator.getReachPreStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getAmountSeenStrategy();
    }
}
