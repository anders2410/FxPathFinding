package paths.factory;

import paths.generator.AlternationGenerator;
import paths.strategy.AlternationStrategy;

public class BiDijkstraSameDistFactory extends BiDijkstraFactory {
    @Override
    public AlternationStrategy getAlternationStrategy() {
        return AlternationGenerator.getSameDistanceStrategy();
    }
}
