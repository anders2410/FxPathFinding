package paths;

import java.util.List;
import java.util.Map;

public class ShortestPathResult {
    public Map<Integer, Integer> pathMap;
    public List<Double> nodeDistance;
    public double d;
    public List<Integer> path;
    public int visitedNodes;
    public long runTime;

    public ShortestPathResult(double d, List<Integer> path, int visitedNodes, long runTime) {
        this.d = d;
        this.path = path;
        this.visitedNodes = visitedNodes;
        this.runTime = runTime;
    }

    public ShortestPathResult(double d, List<Integer> path, int visitedNodes, List<Double> nodedistances, Map<Integer, Integer> pathmap, long runTime) {
        this.d = d;
        this.path = path;
        this.visitedNodes = visitedNodes;
        this.nodeDistance = nodedistances;
        this.pathMap = pathmap;
        this.runTime = runTime;
    }
}
