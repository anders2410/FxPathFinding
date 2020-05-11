package paths.preprocessing;

import javafx.util.Pair;
import model.Graph;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CHResult implements Serializable {
    // We should define our own ID as different machines could generate different ID's
    private static final long serialVersionUID = 6529685098267757690L;

    private final Graph graph;
    private final List<Integer> ranks;
    private final Map<Pair<Integer, Integer>, Integer> shortcuts;

    public CHResult(Graph graph, List<Integer> ranks, Map<Pair<Integer, Integer>, Integer> shortcuts) {
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
