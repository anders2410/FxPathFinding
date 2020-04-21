package paths.strategy;

import model.Graph;
import paths.SSSP;

import java.util.List;

public class ContractionHierarchyPreStrategy implements PreprocessStrategy {
    @Override
    public void process() {
        List<Integer> ranks = SSSP.getNodeRankCH();
        Graph graph = SSSP.getCHGraph();
        SSSP.setGraph(graph);
        if (ranks == null || graph == null) {
            System.out.println("Something wrong with CH preprocess..");
        }
    }
}
