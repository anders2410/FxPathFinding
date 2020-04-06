package paths.generator;

import model.Graph;
import paths.ReachProcessor;
import paths.SSSP;
import paths.strategy.PreprocessStrategy;

public class ReachPreStrategy implements PreprocessStrategy {
    @Override
    public void process() {
        double[] reachBounds = SSSP.getReachBounds();
        Graph g = SSSP.getGraph();
        Graph gg = new Graph(g);
        if (reachBounds == null || reachBounds.length != g.getNodeAmount()) {
            ReachProcessor processor = new ReachProcessor();
            double[] a = processor.computeReachBound(gg);
            SSSP.setReachBounds(a);
        }
        SSSP.setGraph(g);
    }
}
