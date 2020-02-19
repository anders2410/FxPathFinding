package paths;

import java.util.List;

public class ShortestPathResult {
    public double d;
    public List<Integer> path;
    public int visitedNodes;

    public ShortestPathResult(double d, List<Integer> path, int visitedNodes) {
        this.d = d;
        this.path = path;
        this.visitedNodes = visitedNodes;
    }
}
