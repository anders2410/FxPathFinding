package paths.preprocessing;

import javafx.FXMLController;
import model.Edge;
import model.Graph;
import model.ModelUtil;
import model.Node;
import paths.AlgorithmMode;
import paths.SSSP;
import paths.ShortestPathResult;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ReachProcessor {
    private Graph OriginalGraph;
    private List<Double> bounds;
    private double[] reachLCPT;

    private BiConsumer<Long, Long> progressListener = (l1, l2) -> {
    };
    private FXMLController fcontroller;

    double reachMetric(Edge e) {
        //First parameter not useful now, but saved because we might need to do projection later into geometric space (if spherical distance is not provably correct as assumed)
        return e.d;
    }

    public void SetFXML(FXMLController f) {
        fcontroller = f;
    }

    public Graph getOriginalGraph() {
        return OriginalGraph;
    }

    public void setOriginalGraph(Graph originalGraph) {
        this.OriginalGraph = originalGraph;
    }

    long deletedNodes = 0;
    long totalNodes = 0;

    public List<Double> computeReachBound(Graph g) {
        totalNodes = g.getNodeAmount();
        bounds = Arrays.asList(new Double[g.getNodeAmount()]);
        Collections.fill(bounds, Double.MAX_VALUE);
        setOriginalGraph(g);
        Graph subGraph = new Graph(g);
        int[] bIterations = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 16, 20, 25, 30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200, 240, 280, 300};
        Instant start = Instant.now();

        for (int i = 0; i < bIterations.length; i++) {
            if (deletedNodes == 0) {
                progressListener.accept((long) 10 * i, 100L);
            }
            Instant end = Instant.now();
            long timeElapsed = Duration.between(start, end).toMillis();
            start = Instant.now();
            System.out.println("Time: " + timeElapsed);
            subGraph = computeReachBoundsSubgraph(g, subGraph, bIterations[i]);
        }
        /*for (int i = 0; i < 100; i++) {
            if (deletedNodes == 0) {
                progressListener.accept((long) 10 * i, 100L);
            }
            subGraph = computeReachBoundsSubgraph(g, subGraph, i);
        }*/
        SSSP.setGraph(getOriginalGraph());
        return bounds;
    }

    private Graph computeReachBoundsSubgraph(Graph mainGraph, Graph subGraph, int b) {
        System.out.println(b);
        double maxReachOriginalGraph;
        List<Node> originalNodeList = mainGraph.getNodeList();
        List<Node> subGraphNodeList = subGraph.getNodeList();
        reachLCPT = new double[bounds.size()];
        maxReachOriginalGraph = exclusiveOriginalGraphReachBound(mainGraph, subGraph, originalNodeList, subGraphNodeList);
        for (int i = 1; i < subGraphNodeList.size(); i++) {
            if (subGraphNodeList.get(i) != null) {
                bounds.set(i, 0d);
                reachLCPT[i] = 0;
            }
        }
        Graph connectiveGraph = createConnectiveGraph(mainGraph, subGraph);
        Map<Integer, Set<Integer>> nodesIngoingMap = computeGraphExclusiveIn(mainGraph, subGraph);
        SSSP.setGraph(connectiveGraph);
        ModelUtil modelUtil = new ModelUtil(connectiveGraph);
        /*fcontroller.setGraph(connectiveGraph);
        fcontroller.setUpGraph();*/

        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if (b == 0) continue;
            if (subGraphNodeList.get(i) == null) continue;
            double g = 0, d = 0;
            if (nodesIngoingMap.containsKey(i)) {
                for (Integer j : nodesIngoingMap.get(i)) {
                    List<Edge> eList = mainGraph.getAdjList().get(j);
                    Edge e = getEdge(i, eList);
                    g = Math.max(g, bounds.get(j) + reachMetric(e));
                    d = Math.max(d, reachMetric(e));
                }
            }
            double maxFirst = 0;
            for (Edge e : subGraph.getAdjList().get(i)) {
                maxFirst = Math.max(maxFirst, e.d);
            }
            SSSP.setSingleToAllBound(2 * b + maxReachOriginalGraph + d + maxFirst);
       /*     long start1 = System.nanoTime();

            long end1 = System.nanoTime();
            long timeElapsed1 = TimeUnit.MILLISECONDS.convert(end1 - start1, TimeUnit.NANOSECONDS);*/
            Map<Integer, List<Integer>> leastCostTreeH = new HashMap<>();
/*
            long start = System.nanoTime();
*/
            //ShortestPathResult SPTH = SSSP.findShortestPath(i, 300, AlgorithmMode.BOUNDED_SINGLE_TO_ALL);
            Set<Map.Entry<Integer, Integer>> SPT = modelUtil.SPTWithinRadius(i, 2 * b + maxReachOriginalGraph + d + maxFirst).entrySet();
     /*       long end = System.nanoTime();
            long timeElapsed = TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS);*/
            /*System.out.println("SSP -> " + timeElapsed1);
            System.out.println("SSPres -> " + TimeUnit.MILLISECONDS.convert(SPTH.runTime, TimeUnit.NANOSECONDS));
            System.out.println("NotSSP  -> " + timeElapsed);*/
            for (Map.Entry<Integer, Integer> e : SPT) {
                List<Integer> list = leastCostTreeH.computeIfAbsent(e.getValue(), k -> new ArrayList<>());
                list.add(e.getKey());
                leastCostTreeH.replace(e.getValue(), list);
            }
            if (leastCostTreeH.size() == 0) continue;
/*
            Instant start2 = Instant.now();
*/
            traverseTree(leastCostTreeH, subGraph, i, b, maxReachOriginalGraph, g, d);
/*            Instant end2 = Instant.now();
            long timeElapsed2 = Duration.between(start2, end2).toMillis();
            System.out.println("---");
            System.out.println(timeElapsed);
            System.out.println(timeElapsed2);*/
//            int a = 1 + 1;
        }
        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if ((reachLCPT[i] >= b && subGraphNodeList.get(i) != null)) {
                bounds.set(i, Double.MAX_VALUE);
            }
        }
        Graph smallerGraph = new Graph(mainGraph);
        deletedNodes = 0;
        for (int i = 0; i < smallerGraph.getNodeList().size(); i++) {
            if (bounds.get(i) != Double.MAX_VALUE) {
                smallerGraph.getNodeList().set(i, null);
                deletedNodes++;
            }
        }
        progressListener.accept(deletedNodes, totalNodes);
        for (int i = 0; i < smallerGraph.getNodeList().size(); i++) {
            Iterator<Edge> iterator = smallerGraph.getAdjList().get(i).iterator();
            while (iterator.hasNext()) {
                Edge e = iterator.next();
                if (smallerGraph.getNodeList().get(i) == null || smallerGraph.getNodeList().get(e.to) == null)
                    iterator.remove();
            }
        }
        return smallerGraph;
    }

    private void traverseTree(Map<Integer, List<Integer>> leastCostTreeH, Graph graph, int rootNode, int b, double c, double g, double d) {
        double runningMetric = 0.0;
        double metricFirstEdge;
        for (Integer i : leastCostTreeH.get(rootNode)) {
            metricFirstEdge = reachMetric(getEdge(i, getOriginalGraph().getAdjList().get(rootNode)));
            double upperBoundPaths = 2 * b + c + d + metricFirstEdge;
            LinkedHashMap<Integer, Double> sourceNodeMap = new LinkedHashMap<>();
            sourceNodeMap.put(rootNode, 0.0);
            updateBoundsSubTree(leastCostTreeH, rootNode, i, runningMetric, upperBoundPaths, graph, g, sourceNodeMap);
        }
    }

    private double updateBoundsSubTree(Map<Integer, List<Integer>> leastCostTreeH, int parentNode, Integer node, double runningMetric, double upperBoundPaths, Graph subGraph, double g, LinkedHashMap<Integer, Double> sourceNodeMap) {
        double reachMetricLast = reachMetric(getEdge(node, getOriginalGraph().getAdjList().get(parentNode)));
        boolean pathTooLong = runningMetric >= upperBoundPaths + reachMetricLast && sourceNodeMap.size() >= 1;
        boolean endOfPossiblePath = leastCostTreeH.get(node) == null && sourceNodeMap.size() >= 1;
        if (pathTooLong || endOfPossiblePath) {
            if (endOfPossiblePath) runningMetric += reachMetricLast;
            // runningMetric -= reachMetricLast;
            //This condition is equivalent to leaf being found
            double rt = 0;
            if (subGraph.getNodeList().get(node) == null) {
                // leaf is in Supergraph but not subgraph
                rt = bounds.get(node);
            }
            for (Map.Entry<Integer, Double> entry : sourceNodeMap.entrySet()) {
                int key = entry.getKey();
                double value = entry.getValue();
                double nodeToLeaf = runningMetric - value;
                double rb = Math.min(g + value, rt + nodeToLeaf);
                if (rb > bounds.get(key)) {
                    bounds.set(key, rb);
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

        sourceNodeMap.put(node, runningMetric);
        for (Integer i : leastCostTreeH.get(node)) {
            LinkedHashMap<Integer, Double> newsourceNodeMap = new LinkedHashMap<>(sourceNodeMap);
            updateBoundsSubTree(leastCostTreeH, node, i, runningMetric, upperBoundPaths, subGraph, g, newsourceNodeMap);
        }
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
    }

    private double exclusiveOriginalGraphReachBound(Graph g, Graph subGraph, List<Node> originalNodeList, List<Node> subGraphNodeList) {
        double maxReachOriginalGraph = 0;
        if (!g.getNodeList().equals(subGraph.getNodeList())) {
            double maxSoFar = -1;
            for (int i = 0; i < originalNodeList.size(); i++) {
                if (originalNodeList.get(i) != subGraphNodeList.get(i)) {
                    maxSoFar = Math.max(bounds.get(i), maxSoFar);
                }
            }
            maxReachOriginalGraph = maxSoFar;
        }
        return maxReachOriginalGraph;
    }

    public void setProgressListener(BiConsumer<Long, Long> progressListener) {
        this.progressListener = progressListener;
    }
}
