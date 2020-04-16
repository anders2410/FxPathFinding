package paths;

import java.util.*;

public class ShortestPathResult {
    public Map<Integer, Integer> pathMap;
    public List<Double> nodeDistances;
    public Set<Integer> visitedNodesA;
    public Set<Integer> visitedNodesB;
    public double d;
    public List<Integer> path;
    public long runTime;

    public ShortestPathResult() {
        this.d = 0;
        this.path = new ArrayList<>();
        this.visitedNodesA = new HashSet<>();
        this.visitedNodesB = new HashSet<>();
        this.runTime = 0;
    }

    public ShortestPathResult(double d, List<Integer> path, Set<Integer> visitedNodes, long runTime) {
        this.d = d;
        this.path = path;
        this.visitedNodesA = visitedNodes;
        this.visitedNodesB = new HashSet<>();
        this.runTime = runTime;
    }

    public ShortestPathResult(double d, List<Integer> path, Set<Integer> visitedNodesA, Set<Integer> visitedNodesB, long runTime) {
        this.d = d;
        this.path = path;
        this.visitedNodesA = visitedNodesA;
        this.visitedNodesB = visitedNodesB;
        this.runTime = runTime;
    }

    public ShortestPathResult(double d, List<Integer> path, Set<Integer> visitedNodes, List<Double> nodeDistances, Map<Integer, Integer> pathMap, long runTime) {
        this.d = d;
        this.path = path;
        this.visitedNodesA = visitedNodes;
        this.nodeDistances = nodeDistances;
        this.pathMap = pathMap;
        this.runTime = runTime;
    }
}
