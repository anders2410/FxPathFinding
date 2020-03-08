package paths;

import java.util.List;

public class ShortestPathResult {
    public List<Double> nodeDistance;
    public double d;
    public List<Integer> path;
    public int visitedNodes;

    public ShortestPathResult(double d, List<Integer> path, int visitedNodes) {
        this.d = d;
        this.path = path;
        this.visitedNodes = visitedNodes;
    }

    public ShortestPathResult(double d, List<Integer> path, int visitedNodes, List<Double> nodedistances) {
        this.d = d;
        this.path = path;
        this.visitedNodes = visitedNodes;
        this.nodeDistance = nodedistances;
    }
}
