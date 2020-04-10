package paths;

import load.GraphIO;
import model.Edge;
import model.Graph;
import model.Node;
import model.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public class ReachProcessor {
    private Graph OriginalGraph;
    private double[] bounds;
    private double[] reachLCPT;

    double reachMetric(int nodeFrom, Edge e) {
        return e.d;
    }

    public Graph getOriginalGraph() {
        return OriginalGraph;
    }

    public void setOriginalGraph(Graph originalGraph) {
        this.OriginalGraph = originalGraph;
    }

    public double[] computeReachBound(Graph g) {
        bounds = new double[g.getNodeAmount()];
        Arrays.fill(bounds, Double.MAX_VALUE);
        setOriginalGraph(g);
        Graph subGraph = new Graph(g);
        for (int i = 0; i < 100; i++) {
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
        for (int i = 1; i < subGraphNodeList.size(); i++) {
            if (subGraphNodeList.get(i) != null) {
                bounds[i] = 0;
                reachLCPT[i] = 0;
            }
        }
        Graph connectiveGraph = createConnectiveGraph(mainGraph, subGraph);
        Map<Integer, Set<Integer>> nodesIngoingMap = computeGraphExclusiveIn(mainGraph, subGraph);
        SSSP.setGraph(connectiveGraph);

        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if (subGraphNodeList.get(i) == null) continue;
            double g = 0, d = 0;
            if (nodesIngoingMap.containsKey(i)) {
                for (Integer j : nodesIngoingMap.get(i)) {
                    List<Edge> eList = mainGraph.getAdjList().get(j);
                    Edge e = getEdge(i, eList);
                    g = Math.max(g, bounds[j] + reachMetric(j, e));
                    d = Math.max(d, reachMetric(j, e));
                }
            }
            ShortestPathResult SPTH = SSSP.singleToAllPath(i);
            Map<Integer, List<Integer>> leastCostTreeH = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : SPTH.pathMap.entrySet()) {
                List<Integer> list = leastCostTreeH.computeIfAbsent(e.getValue(), k -> new ArrayList<>());
                list.add(e.getKey());
                leastCostTreeH.replace(e.getValue(), list);
            }
/*
            Map<Integer, List<Integer>> leastCostTreeH = SPTH.pathMap.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
*/
            if (leastCostTreeH.size() == 0) continue;
            traverseTree(leastCostTreeH, subGraph, i, b, maxReachOriginalGraph, g, d);
        }
        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if ( (reachLCPT[i] >= b && subGraphNodeList.get(i) != null)) {
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
                if (smallerGraph.getNodeList().get(i) == null || smallerGraph.getNodeList().get(e.to) == null)
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
            metricFirstEdge = reachMetric(rootNode, getEdge(i, getOriginalGraph().getAdjList().get(rootNode)));
            double upperBoundPaths = 2 * b + c + d + metricFirstEdge;
            LinkedHashMap<Integer, Double> sourceNodeMap = new LinkedHashMap<>();
            sourceNodeMap.put(rootNode, 0.0);
            updateBoundsSubTree(leastCostTreeH, rootNode, i, runningMetric, upperBoundPaths, graph, g, sourceNodeMap);
        }
    }

    private double updateBoundsSubTree(Map<Integer, List<Integer>> leastCostTreeH, int parentNode, Integer node, double runningMetric, double upperBoundPaths, Graph graph, double g, LinkedHashMap<Integer, Double> sourceNodeMap) {
        double reachMetricLast = reachMetric(parentNode, getEdge(node, getOriginalGraph().getAdjList().get(parentNode)));
        boolean pathTooLong = runningMetric >= upperBoundPaths + reachMetricLast && sourceNodeMap.size() >= 1;
        boolean endOfPossiblePath = leastCostTreeH.get(node) == null && sourceNodeMap.size() >= 1;
        if (pathTooLong || endOfPossiblePath) {
            if (endOfPossiblePath) runningMetric += reachMetricLast;
            // runningMetric -= reachMetricLast;
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
                double min = Math.min(value, nodeToLeaf);
                if (min > reachLCPT[key]) reachLCPT[key] = min;
            }
            return runningMetric;
        }
        if (leastCostTreeH.get(node) == null) {
            return 0;
        }
        runningMetric += reachMetricLast;
        //int subpaths = leastCostTreeH.get(node).size();
        //double[] maxPathLengths = new double[leastCostTreeH.get(node).size()];
        //int arrayIndex = 0;
        sourceNodeMap.put(node, runningMetric);
        for (Integer i : leastCostTreeH.get(node)) {
            LinkedHashMap<Integer, Double> newsourceNodeMap = new LinkedHashMap<>(sourceNodeMap);
            updateBoundsSubTree(leastCostTreeH, node, i, runningMetric, upperBoundPaths, graph, g, newsourceNodeMap);
            //maxPathLengths[arrayIndex] = runningPathReachMetric;
            //arrayIndex++;
        }
        /*double maxPath = 0;
        for (double maxPathLength : maxPathLengths) {
            maxPath = Math.max(maxPath, maxPathLength);
        }
        for (Map.Entry<Integer, Double> entry : sourceNodeMap.entrySet()) {
            int key = entry.getKey();
            double value = entry.getValue();
            double nodeToLeaf = maxPath - value;
            double min = Math.min(value, nodeToLeaf);
            if (min > reachLCPT[key]) reachLCPT[key] = min;
        }*/
        return 0;
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
                boolean b = !subGraph.getAdjList().get(i).contains(e);
                if (b) {
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
        for (int i = 0; i < g.getAdjList().size(); i++) {
            for (Edge e : g.getAdjList().get(i)) {
                if (subGraph.getNodeList().get(i) != null) {
                    connectiveGraph.getAdjList().get(i).add(e);
                    connectiveGraph.getNodeList().set(e.to, g.getNodeList().get(e.to));
                }
            }

        }
        return connectiveGraph;
        /*Graph connectiveGraph = new Graph(g.getNodeAmount());
        connectiveGraph.setNodeList(new ArrayList<>(subGraph.getNodeList()));
        for (int i = 0; i < subGraph.getNodeList().size(); i++) {
            for (int j = 0; j < g.getNodeList().size(); j++) {
                if (subGraph.getNodeList().get(i) != null) {
                    connectiveGraph.getAdjList().get(i).add(new Edge(j, Util.sphericalDistance(subGraph.getNodeList().get(i), subGraph.getNodeList().get(j))));

                    connectiveGraph.getNodeList().set(i, new Node(g.getNodeList().get(i)));
                }
            }

        }
        return connectiveGraph;*/
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
