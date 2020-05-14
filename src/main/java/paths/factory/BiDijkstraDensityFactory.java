package paths.factory;

import paths.generator.AlternationGenerator;
import paths.generator.ScanPruningGenerator;
import paths.generator.TerminationGenerator;
import paths.strategy.AlternationStrategy;
import paths.strategy.ScanPruningStrategy;
import paths.strategy.TerminationStrategy;

public class BiDijkstraDensityFactory extends BiDijkstraFactory {
    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getDensityBasedStrategy();
    }
    @Override
    public TerminationStrategy getTerminationStrategy() {
        return TerminationGenerator.getKeyAboveGoalStrategy();
    }
}
