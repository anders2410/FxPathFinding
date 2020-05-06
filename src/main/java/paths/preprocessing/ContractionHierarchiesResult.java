package paths.preprocessing;

import javafx.util.Pair;
import model.Graph;

import java.util.List;
import java.util.Map;

public class ContractionHierarchiesResult {
    private Graph graph;
    private List<Integer> ranks;
    private Map<Pair<Integer, Integer>, Integer> shortcuts;

    public ContractionHierarchiesResult(Graph graph, List<Integer> ranks, Map<Pair<Integer, Integer>, Integer> shortcuts) {
        this.graph = graph;
        this.ranks = ranks;
        this.shortcuts = shortcuts;
    }

    public Graph getGraph() {
        return graph;
    }

    public List<Integer> getRanks() {
        return ranks;
    }

    public Map<Pair<Integer, Integer>, Integer> getShortcuts() {
        return shortcuts;
    }
}
