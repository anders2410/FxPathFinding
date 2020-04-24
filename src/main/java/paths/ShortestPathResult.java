package paths;

import model.Edge;
import model.Graph;

import java.util.*;

public class ShortestPathResult {
    public Map<Integer, Integer> pathMap;
    public List<Double> nodeDistances;
    public Set<Integer> scannedNodesA;
    public Set<Integer> scannedNodesB;
    public double d;
    public List<Integer> path;
    public long runTime;

    public ShortestPathResult() {
        this.d = 0;
        this.path = new ArrayList<>();
        this.scannedNodesA = new HashSet<>();
        this.scannedNodesB = new HashSet<>();
        this.runTime = 0;
    }

    public ShortestPathResult(double d, List<Integer> path, Set<Integer> visitedNodes, long runTime) {
        this.d = d;
        this.path = path;
        this.scannedNodesA = visitedNodes;
        this.scannedNodesB = new HashSet<>();
        this.runTime = runTime;
    }

    public ShortestPathResult(double d, List<Integer> path, Set<Integer> scannedNodesA, Set<Integer> scannedNodesB, long runTime) {
        this.d = d;
        this.path = path;
        this.scannedNodesA = scannedNodesA;
        this.scannedNodesB = scannedNodesB;
        this.runTime = runTime;
    }

    public ShortestPathResult(double d, List<Integer> path, Set<Integer> visitedNodes, List<Double> nodeDistances, Map<Integer, Integer> pathMap, long runTime) {
        this.d = d;
        this.path = path;
        this.scannedNodesA = visitedNodes;
        this.nodeDistances = nodeDistances;
        this.pathMap = pathMap;
        this.runTime = runTime;
    }

    public int calculateAllUniqueVisits(Graph g) {
        Set<Integer> visits = new HashSet<>(scannedNodesA);
        visits.addAll(scannedNodesB);
        for (int i = 0; i < g.getNodeList().size(); i++) {
            if (scannedNodesA.contains(i)) {
                for (Edge e : g.getAdjList().get(i)) {
                    visits.add(e.to);
                }
            }
        }
        return visits.size();
    }


}
