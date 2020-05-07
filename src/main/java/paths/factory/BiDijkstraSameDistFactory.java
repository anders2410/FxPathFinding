package paths.factory;

import paths.generator.AlternationGenerator;
import paths.generator.ScanPruningGenerator;
import paths.generator.TerminationGenerator;
import paths.strategy.AlternationStrategy;
import paths.strategy.ScanPruningStrategy;
import paths.strategy.TerminationStrategy;

public class BiDijkstraSameDistFactory extends BiDijkstraFactory {
    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getSameDistanceStrategy();
    }
    @Override
    public ScanPruningStrategy getScanPruningStrategy() {
        return ScanPruningGenerator.getBasePruning();
    }
    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getSearchMeetTermination();
    }
}
