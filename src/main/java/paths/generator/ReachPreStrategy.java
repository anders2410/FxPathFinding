package paths.generator;

import javafx.FXMLController;
import model.Edge;
import model.Graph;
import paths.ReachProcessor;
import paths.SSSP;
import paths.strategy.PreprocessStrategy;

import java.util.Iterator;

public class ReachPreStrategy implements PreprocessStrategy {
    @Override
    public void process() {
        double[] reachBounds = SSSP.getReachBounds();
        if (reachBounds == null) {
            System.out.println("No reach bounds found for graph");
        }
    }
}
