package paths.factory;

import paths.generator.*;
import paths.strategy.*;

public class ContractionHierarchiesLandmarksFactory implements AlgorithmFactory {
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
        return RelaxGenerator.getCH();
    }

    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getEmptyStoppingStrategy();
    }

    @Override
    public ScanPruningStrategy getScanPruningStrategy() {
        return ScanPruningGenerator.getCHPruning();
    }

    @Override
    public ResultPackingStrategy getResultPackingStrategy() {
        return ResultPackingGenerator.getCHPack();
    }

    @Override
    public PreProcessStrategy getPreProcessStrategy() {
        return PreProcessGenerator.getCHLandmarksStrategy();
    }

    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getBiggestQueueStrategy();
    }
}

