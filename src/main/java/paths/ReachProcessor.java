package paths;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;
import java.util.stream.Collectors;

public class ReachProcessor {
    private Graph graph;

    double reachMetric(int nodeFrom, Edge e) {
        return e.d;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public void computeReachBound(Graph g, int[] boundCeilingArr) {
        double[] bounds = new double[g.getNodeAmount()];
        Arrays.fill(bounds, Double.MAX_VALUE);
        Graph subGraph = g;
        for (int i = 0; i < 100; i++) {
            subGraph = computeReachBoundsSubgraph(g, subGraph, i, bounds);
        }
    }

    private Graph computeReachBoundsSubgraph(Graph mainGraph, Graph subGraph, int b, double[] bounds) {
        double maxReachOriginalGraph;
        List<Node> originalNodeList = mainGraph.getNodeList();
        List<Node> subGraphNodeList = subGraph.getNodeList();
        //reachSPT is reach of nodes in least-cost path trees. (SPT != least-cost, but close). Least-cost uses reach metric, SPT uses weight metric.
        double[] reachLCPT = new double[bounds.length];
        maxReachOriginalGraph = exclusiveOriginalGraphReachBound(mainGraph, subGraph, bounds, originalNodeList, subGraphNodeList);
        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if (subGraphNodeList.get(i) != null) {
                bounds[i] = 0;
                reachLCPT[i] = 0;
            }
        }
        Graph connectiveGraph = createConnectiveGraph(mainGraph, subGraph);
        Map<Integer, Set<Integer>> nodesIngoingMap = computeGraphExclusiveIn(mainGraph, subGraph);

        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if (subGraphNodeList.get(i) == null) continue;
            double g = 0, d = 0;
            if (nodesIngoingMap.containsKey(i)) {
                double gMax = 0;
                double dMax = 0;
                for (Integer j : nodesIngoingMap.get(i)) {
                    List<Edge> eList = subGraph.getAdjList().get(j);
                    Edge e = getEdge(i, eList);
                    gMax = Math.max(gMax, bounds[j] + reachMetric(j, e));
                    dMax = Math.max(dMax, reachMetric(j, e));
                }
            }
            SSSP.setGraph(connectiveGraph);
            ShortestPathResult SPTH = SSSP.singleToAllPath(i);
            Map<Integer, List<Integer>> leastCostTreeH = SPTH.pathMap.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
            traverseTree(leastCostTreeH, connectiveGraph, i, b, maxReachOriginalGraph, g, d);
        }

        return null;
    }

    private void traverseTree(Map<Integer, List<Integer>> leastCostTreeH, Graph graph, int rootNode, int b, double c, double g, double d) {
        double pathMetric = 0.0;
        //r(v,T)
        double rvt = 0.0;
        double runningMetric = 0.0;
        double metricFirstEdge;
        for (Integer i : leastCostTreeH.get(rootNode)) {
            metricFirstEdge = reachMetric(rootNode, getEdge(i, graph.getAdjList().get(rootNode)));
            traverseSubTree(leastCostTreeH, rootNode, i, runningMetric, metricFirstEdge, graph, b, c, g, d);
        }
    }

    private double traverseSubTree(Map<Integer, List<Integer>> leastCostTreeH, int parentNode, Integer node, double runningMetric, double metricFirstEdge, Graph graph, int b, double c, double g, double d) {
        double reachMetricLast = reachMetric(parentNode, getEdge(parentNode, graph.getAdjList().get(node)));
        runningMetric += reachMetricLast;
        double reachSourceNode = runningMetric;
        if (runningMetric >= 2 * b + c + d + metricFirstEdge + reachMetricLast) {
            return runningMetric - reachMetricLast;
        }
        return reachMetricLast;
    }

    private Edge getEdge(int i, List<Edge> eList) {
        Edge e = null;
        for (Edge edge : eList) {
            if (edge.to == i) {
                e = edge;
                break;
            }
        }
        return e;
    }

    private Map<Integer, Set<Integer>> computeGraphExclusiveIn(Graph g, Graph subGraph) {
        Map<Integer, Set<Integer>> nodesIngoingMap = new HashMap<>();
        for (int i = 0; i < g.getNodeList().size(); i++) {
            for (Edge e : g.getAdjList().get(i)) {
                if (!subGraph.getAdjList().get(i).contains(e)) {
                    Set<Integer> nodesInto = nodesIngoingMap.computeIfAbsent(e.to, k -> new HashSet<>());
                    nodesInto.add(i);
                    nodesIngoingMap.replace(e.to, nodesInto);
                }
            }
        }
        return nodesIngoingMap;
    }

    private Graph createConnectiveGraph(Graph g, Graph subGraph) {
        Graph connectiveGraph = new Graph(g.getNodeAmount());
        connectiveGraph.setNodeList(subGraph.getNodeList());
        for (int i = 0; i < subGraph.getAdjList().size(); i++) {
            for (Edge e : subGraph.getAdjList().get(i)) {
                if (subGraph.getNodeList().get(e.to) == null) {
                    connectiveGraph.getAdjList().get(i).add(e);
                    connectiveGraph.getNodeList().set(i, g.getNodeList().get(e.to));
                }
            }

        }
        return connectiveGraph;
    }

    private double exclusiveOriginalGraphReachBound(Graph g, Graph subGraph, double[] bounds, List<Node> originalNodeList, List<Node> subGraphNodeList) {
        double maxReachOriginalGraph = 0;
        if (!g.getNodeList().equals(subGraph.getNodeList())) {
            double maxSoFar = -1;
            for (int i = 0; i < originalNodeList.size(); i++) {
                if (originalNodeList.get(i) != subGraphNodeList.get(i)) {
                    maxSoFar = Math.max(bounds[i], maxSoFar);
                }
            }
            maxReachOriginalGraph = maxSoFar;
        }
        return maxReachOriginalGraph;
    }

}
