package paths;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;
import java.util.stream.Collectors;

public class ReachProcessor {
    private Graph graph;
    private double[] bounds;
    private double[] reachLCPT;

    double reachMetric(int nodeFrom, Edge e) {
        return e.d;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public double[] computeReachBound(Graph g) {
        bounds = new double[g.getNodeAmount()];
        Arrays.fill(bounds, Double.MAX_VALUE);
        Graph subGraph = new Graph(g);
        for (int i = 0; i < 100; i++) {
            System.out.println(i);
            subGraph = computeReachBoundsSubgraph(g, subGraph, i);
        }
        return bounds;
    }

    private Graph computeReachBoundsSubgraph(Graph mainGraph, Graph subGraph, int b) {
        double maxReachOriginalGraph;
        List<Node> originalNodeList = mainGraph.getNodeList();
        List<Node> subGraphNodeList = subGraph.getNodeList();
        //reachSPT is reach of nodes in least-cost path trees. (SPT != least-cost, but close). Least-cost uses reach metric, SPT uses weight metric.
        reachLCPT = new double[bounds.length];
        maxReachOriginalGraph = exclusiveOriginalGraphReachBound(mainGraph, subGraph, originalNodeList, subGraphNodeList);
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
            if (leastCostTreeH.size() == 0) continue;
            traverseTree(leastCostTreeH, connectiveGraph, i, b, maxReachOriginalGraph, g, d);
        }
        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if (reachLCPT[i] >= b) {
                bounds[i] = Double.MAX_VALUE;
            }
        }
        Graph smallerGraph = new Graph(mainGraph);
        for (int i = 0; i < smallerGraph.getNodeList().size(); i++) {
            if (bounds[i] != Double.MAX_VALUE) {
                smallerGraph.getNodeList().set(i, null);
            }
        }
        for (int i = 0; i < smallerGraph.getNodeList().size(); i++) {
            Iterator<Edge> iterator = smallerGraph.getAdjList().get(i).iterator();
            while (iterator.hasNext()) {
                Edge e = iterator.next();
                if (smallerGraph.getNodeList().get(i) == null && smallerGraph.getNodeList().get(e.to) == null)
                    iterator.remove();
            }
        }
        // TODO: 05-04-2020 Validation check remains
        return smallerGraph;
    }

    private void traverseTree(Map<Integer, List<Integer>> leastCostTreeH, Graph graph, int rootNode, int b, double c, double g, double d) {
        double runningMetric = 0.0;
        double metricFirstEdge;

        for (Integer i : leastCostTreeH.get(rootNode)) {
            metricFirstEdge = reachMetric(rootNode, getEdge(i, graph.getAdjList().get(rootNode)));
            double upperBoundPaths = 2 * b + c + d + metricFirstEdge;
            LinkedHashMap<Integer, Double> sourceNodeMap = new LinkedHashMap<>();
            sourceNodeMap.put(rootNode, 0.0);
            updateBoundsSubTree(leastCostTreeH, rootNode, i, runningMetric, upperBoundPaths, graph, g, sourceNodeMap);
        }
    }

    private double updateBoundsSubTree(Map<Integer, List<Integer>> leastCostTreeH, int parentNode, Integer node, double runningMetric, double upperBoundPaths, Graph graph, double g, LinkedHashMap<Integer, Double> sourceNodeMap) {
        double reachMetricLast = reachMetric(parentNode, getEdge(node, graph.getAdjList().get(parentNode)));
        runningMetric += reachMetricLast;
        if (runningMetric >= upperBoundPaths + reachMetricLast || leastCostTreeH.get(node) == null) {
            //This condition is equivalent to leaf being found
            double rt = 0;
            if (graph.getNodeList().get(node) == null) {
                // leaf is in Supergraph but not subgraph
                rt = bounds[node];
            }
            for (Map.Entry<Integer, Double> entry : sourceNodeMap.entrySet()) {
                int key = entry.getKey();
                double value = entry.getValue();
                double nodeToLeaf = runningMetric - value;
                double rb = Math.min(g + value, rt + nodeToLeaf);
                if (rb > bounds[key]) {
                    bounds[key] = rb;
                }
            }

            return runningMetric - reachMetricLast;
        }
        int subpaths = leastCostTreeH.get(node).size();
        double[] maxPathLengths = new double[leastCostTreeH.get(node).size()];
        int arrayIndex = 0;
        sourceNodeMap.put(node, runningMetric);
        for (Integer i : leastCostTreeH.get(node)) {
            LinkedHashMap<Integer, Double> newsourceNodeMap = new LinkedHashMap<>(sourceNodeMap);
            double runningPathReachMetric = updateBoundsSubTree(leastCostTreeH, node, i, runningMetric, upperBoundPaths, graph, g, newsourceNodeMap);
            maxPathLengths[arrayIndex] = runningPathReachMetric;
            arrayIndex++;
        }
        double maxPath = 0;
        for (double maxPathLength : maxPathLengths) {
            maxPath = Math.max(maxPath, maxPathLength);
        }
        for (Map.Entry<Integer, Double> entry : sourceNodeMap.entrySet()) {
            int key = entry.getKey();
            double value = entry.getValue();
            double nodeToLeaf = runningMetric - value;
            double min = Math.min(value, nodeToLeaf);
            if (min > reachLCPT[key]) reachLCPT[key] = min;
        }
        return maxPath;
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

    public ReachProcessor() {
    }

    private Graph createConnectiveGraph(Graph g, Graph subGraph) {
        Graph connectiveGraph = new Graph(g.getNodeAmount());
        connectiveGraph.setNodeList(new ArrayList<>(subGraph.getNodeList()));
        for (int i = 0; i < subGraph.getAdjList().size(); i++) {
            for (Edge e : subGraph.getAdjList().get(i)) {
                if (g.getNodeList().get(e.to) != null) {
                    connectiveGraph.getAdjList().get(i).add(e);
                    connectiveGraph.getNodeList().set(i, g.getNodeList().get(e.to));
                }
            }

        }
        return connectiveGraph;
    }

    private double exclusiveOriginalGraphReachBound(Graph g, Graph subGraph, List<Node> originalNodeList, List<Node> subGraphNodeList) {
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
