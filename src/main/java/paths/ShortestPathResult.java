package paths;

import java.util.List;
import java.util.Map;

public class ShortestPathResult {
    public Map<Integer, Integer> pathMap;
    public List<Double> nodeDistance;
    public double d;
    public List<Integer> path;
    public int visitedNodes;

    public ShortestPathResult(double d, List<Integer> path, int visitedNodes) {
        this.d = d;
        this.path = path;
        this.visitedNodes = visitedNodes;
    }

    public ShortestPathResult(double d, List<Integer> path, int visitedNodes, List<Double> nodedistances, Map<Integer, Integer> pathmap) {
        this.d = d;
        this.path = path;
        this.visitedNodes = visitedNodes;
        this.nodeDistance = nodedistances;
        this.pathMap = pathmap;
    }
}
