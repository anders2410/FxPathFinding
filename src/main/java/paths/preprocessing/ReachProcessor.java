package paths.preprocessing;

import javafx.FXMLController;
import model.Edge;
import model.Graph;
import model.Node;
import paths.SSSP;

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
    private double[] nodeDistLCPT;

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
        int[] bIterations = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 23, 36, 47, 60, 80, 100, 140, 180, 240, 300, 350, 400, 500, 600, 700};
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

            long start = System.nanoTime();
            //ShortestPathResult SPTH = SSSP.findShortestPath(i, 300, AlgorithmMode.BOUNDED_SINGLE_TO_ALL);
            BoundedSPTResult boundedSPTResult = SPTWithinRadius(i, 2 * b + maxReachOriginalGraph + d + maxFirst, connectiveGraph);
            Set<Map.Entry<Integer, Integer>> SPT = boundedSPTResult.pathMap.entrySet();
            long end = System.nanoTime();
            long timeElapsed = TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS);
            /*System.out.println("SSP -> " + timeElapsed1);
            System.out.println("SSPres -> " + TimeUnit.MILLISECONDS.convert(SPTH.runTime, TimeUnit.NANOSECONDS));
            System.out.println("NotSSP  -> " + timeElapsed);*/
            for (Map.Entry<Integer, Integer> e : SPT) {
                List<Integer> list = leastCostTreeH.computeIfAbsent(e.getValue(), k -> new ArrayList<>());
                list.add(e.getKey());
                leastCostTreeH.replace(e.getValue(), list);
            }
            if (leastCostTreeH.size() == 0) continue;
            nodeDistLCPT = boundedSPTResult.nodeDist;
            long traverseTime = System.nanoTime();
            traverseTree(leastCostTreeH, subGraph, i, b, maxReachOriginalGraph, g, d);
            //nonRetardTraverseTree(leastCostTreeH, subGraph, i, b, maxReachOriginalGraph, g, d, boundedSPTResult, i);
            long traverseEnd = System.nanoTime();
            long timeElapsed2 = TimeUnit.MILLISECONDS.convert(traverseEnd - traverseTime, TimeUnit.NANOSECONDS);
            /*if (b > 2) {
                System.out.println("---");
                System.out.println("b :" + b + " oneToAll -> " + timeElapsed);
                System.out.println("b :" + b + " TreeTraverse -> " + timeElapsed2);
            }*/
            //if (b > 2) System.out.println("treeTraverse -> " + timeElapsed2);
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

    public BoundedSPTResult SPTWithinRadius(int source, double radius, Graph graph) {
        double[] nodeDist = new double[graph.getNodeAmount()];
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            nodeDist[i] = Double.MAX_VALUE;
        }
        nodeDist[source] = 0.0;
        Map<Integer, Integer> pathMap = new HashMap<>();
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(Comparator.comparing((integer -> nodeDist[integer])));
        /*for (Node node : graph.getNodeList()) {
            if (node != null) priorityQueue.add(node.index);
        }*/
        priorityQueue.add(source);
        List<Node> nList = graph.getNodeList();
        while (!priorityQueue.isEmpty() && (nodeDist[priorityQueue.peek()]) < radius) {
            Integer from = priorityQueue.poll();
            for (Edge edge : graph.getAdjList().get(from)) {
                if (nList.get(edge.to) == null) continue;
                if (nodeDist[from] + edge.d < nodeDist[edge.to]) {
                    nodeDist[edge.to] = nodeDist[from] + edge.d;
                    priorityQueue.remove(edge.to);
                    priorityQueue.add(edge.to);
                    pathMap.put(edge.to, edge.from);
                }
            }
        }
        return new BoundedSPTResult(pathMap, nodeDist);
    }

    private void traverseTree(Map<Integer, List<Integer>> leastCostTreeH, Graph graph, int rootNode, int b, double c, double g, double d) {
        double runningMetric = 0.0;
        double metricFirstEdge;
        for (Integer i : leastCostTreeH.get(rootNode)) {
            metricFirstEdge = reachMetric(getEdge(i, getOriginalGraph().getAdjList().get(rootNode)));
            double upperBoundPaths = 2 * b + c + d + metricFirstEdge;
            List<Integer> nodesInPathSet = new ArrayList<>();
            nodesInPathSet.add(rootNode);
            updateBoundsSubTree(leastCostTreeH, rootNode, i, runningMetric, upperBoundPaths, graph, g, nodesInPathSet);
        }
    }

    private void nonRetardTraverseTree(Map<Integer, List<Integer>> leastCostTreeH, Graph graph, int rootNode, int b, double c, double g, double d, BoundedSPTResult SPTRes, Integer treeRoot) {
        double metricFirstEdge = 0;
        List<Node> nodeList = graph.getNodeList();
        for (Edge e : graph.getAdjList().get(rootNode)) {
            metricFirstEdge = Math.max(metricFirstEdge, e.d);
        }
        double upperBoundPaths = 2 * b + c + d + metricFirstEdge;
        //traverseTree(leastCostTreeH, subGraph, i, b, maxReachOriginalGraph, g, d);
        for (int i = 0; i < nodeList.size(); i++) {
            if (nodeList.get(i) == null) continue;
            if (!SPTRes.pathMap.containsKey(i)) continue;
            nonRetardedUpdateBoundsSubTree(leastCostTreeH, SPTRes.pathMap.get(i), i, upperBoundPaths, graph, g, SPTRes, treeRoot);
        }

       /* long traverseTime = System.nanoTime();
        long traverseEnd = System.nanoTime();
        long timeElapsed2 = TimeUnit.MILLISECONDS.convert(traverseEnd - traverseTime, TimeUnit.NANOSECONDS);
        if (b > 3) System.out.println("treeTraverse -> " + timeElapsed2);*/
    }

    private double updateBoundsSubTree(Map<Integer, List<Integer>> leastCostTreeH, int parentNode, Integer node, double runningMetric, double upperBoundPaths, Graph subGraph, double g, List<Integer> nodesInPath) {
        double reachMetricLast = reachMetric(getEdge(node, getOriginalGraph().getAdjList().get(parentNode)));
        boolean pathTooLong = runningMetric >= upperBoundPaths + reachMetricLast && nodesInPath.size() >= 1;
        boolean endOfPossiblePath = leastCostTreeH.get(node) == null && nodesInPath.size() >= 1;
        if (pathTooLong || endOfPossiblePath) {
            if (endOfPossiblePath) runningMetric += reachMetricLast;
            // runningMetric -= reachMetricLast;
            // This condition is equivalent to leaf being found
            double rt = 0;
            if (subGraph.getNodeList().get(node) == null) {
                // leaf is in Supergraph but not subgraph
                rt = bounds.get(node);
            }
            double lengthToLeaf = nodeDistLCPT[node];
            /*nodesInPath.parallelStream().forEach(nodeInPath -> {
                double rootToNode = nodeDistLCPT[nodeInPath];
                double nodeToLeaf = lengthToLeaf - rootToNode;
                double rb = Math.min(g + rootToNode, finalRt + nodeToLeaf);
                if (rb > bounds.get(nodeInPath)) {
                    bounds.set(nodeInPath, rb);
                }
                double min = Math.min(rootToNode, nodeToLeaf);
                if (min > reachLCPT[nodeInPath]) reachLCPT[nodeInPath] = min;
            });*/
            for (int i = 0; i < nodesInPath.size(); i++) {
                int nodeInPath = nodesInPath.get(i);
                double rootToNode = nodeDistLCPT[nodeInPath];
                double nodeToLeaf = lengthToLeaf - rootToNode;
                double rb = Math.min(g + rootToNode, rt + nodeToLeaf);
                if (rb > bounds.get(nodeInPath)) {
                    bounds.set(nodeInPath, rb);
                }
                double min = Math.min(rootToNode, nodeToLeaf);
                if (min > reachLCPT[nodeInPath]) reachLCPT[nodeInPath] = min;
            }
            return runningMetric;
        }
        if (leastCostTreeH.get(node) == null) {
            return 0;
        }
        runningMetric += reachMetricLast;

        nodesInPath.add(node);
        for (Integer i : leastCostTreeH.get(node)) {
            List<Integer> newNodesInPathSet = new ArrayList<>(nodesInPath);
            updateBoundsSubTree(leastCostTreeH, node, i, runningMetric, upperBoundPaths, subGraph, g, newNodesInPathSet);
        }
        return 0;
    }

    private void nonRetardedUpdateBoundsSubTree(Map<Integer, List<Integer>> leastCostTreeH, int parentNode, Integer node, double upperBoundPaths, Graph subGraph, double g, BoundedSPTResult SPTRes, Integer treeRoot) {
        double reachMetricLast = getEdge(node, getOriginalGraph().getAdjList().get(parentNode)).d;
        Double lengthToLeaf = SPTRes.nodeDist[node];
        boolean pathTooLong = lengthToLeaf >= upperBoundPaths + reachMetricLast;
        boolean endOfPossiblePath = leastCostTreeH.get(node) == null;
        if (pathTooLong || endOfPossiblePath) {
            // runningMetric -= reachMetricLast;
            // This condition is equivalent to leaf being found
            double rt = 0;
            if (subGraph.getNodeList().get(node) == null) {
                // leaf is in Supergraph but not subgraph
                rt = bounds.get(node);
            }


            long traverseTime = System.nanoTime();
            List<Integer> nodesInPathList = findNodesInPath(SPTRes.pathMap, treeRoot, node);
            if (pathTooLong) lengthToLeaf -= reachMetricLast;
           /* long traverseEnd = System.nanoTime();
            long timeElapsed2 = TimeUnit.MILLISECONDS.convert(traverseEnd - traverseTime, TimeUnit.NANOSECONDS);
            if (timeElapsed2 >= 1) {
                System.out.println(timeElapsed2);
            }*/

            for (Integer nodeInPath : nodesInPathList) {
                double rootToNode = SPTRes.nodeDist[nodeInPath];
                double nodeToLeaf = lengthToLeaf - rootToNode;
                double rb = Math.min(g + rootToNode, rt + nodeToLeaf);
                if (rb > bounds.get(nodeInPath)) {
                    bounds.set(nodeInPath, rb);
                }
                double min = Math.min(rootToNode, nodeToLeaf);
                if (min > reachLCPT[node]) reachLCPT[node] = min;
            }
        }
    }

    private List<Integer> findNodesInPath(Map<Integer, Integer> pathMap, int from, int to) {
        Integer curNode = to;
        List<Integer> path = new ArrayList<>(pathMap.size());
        path.add(to);
        while (curNode != from) {
            curNode = pathMap.get(curNode);
            if (curNode == null) {
                return new ArrayList<>(0);
            }
            path.add(curNode);
        }
        path.remove(path.size() - 1);
        return path;
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

    private class BoundedSPTResult {
        Map<Integer, Integer> pathMap;
        double[] nodeDist;

        public BoundedSPTResult(Map<Integer, Integer> pathMap, double[] nodeDist) {
            this.pathMap = pathMap;
            this.nodeDist = nodeDist;
        }
    }
}
