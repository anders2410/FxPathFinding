package paths.generator;

import paths.SSSP;
import paths.strategy.PreprocessStrategy;

import java.util.List;

public class ReachPreStrategy implements PreprocessStrategy {
    @Override
    public void process() {
        List<Double> reachBounds = SSSP.getReachBounds();
        if (reachBounds == null) {
            System.out.println("No reach bounds found for graph");
        }
    }
}
