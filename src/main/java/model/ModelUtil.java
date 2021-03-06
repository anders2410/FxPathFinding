package model;

import info_model.*;
import paths.ABDir;
import paths.ShortestPathResult;
import paths.Util;

import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.Integer.max;
import static paths.SSSP.*;

public class ModelUtil {

    private Graph graph;

    private BiConsumer<Long, Long> progressListener = (l1, l2) -> {
    };

    public ModelUtil(Graph graph) {
        this.graph = graph;
    }

    public int[] bfsMaxDistance(int startNode) {
        int n = graph.getNodeAmount();
        List<List<Edge>> adjList = graph.getAdjList();
        int[] hop = new int[n];
        Arrays.fill(hop, Integer.MAX_VALUE);
        boolean[] seen = new boolean[n];
        Queue<Integer> queue = new ArrayDeque<>(n);
        queue.add(startNode);
        hop[startNode] = 0;
        seen[startNode] = true;
        while (!queue.isEmpty()) {
            int top = queue.poll();
            for (Edge e : adjList.get(top)) {
                if (!seen[e.to]) {
                    hop[e.to] = hop[top] + 1;
                    queue.add(e.to);
                    seen[e.to] = true;
                }
            }
        }
        return hop;
    }

    /**
     * @param radius to scan in kilometer from the nodes.
     * @return list of amount of neighbours in radius for all nodes.
     */
    public List<Integer> computeDensityMeasures(double radius) {
        List<Integer> densityMeasures = new ArrayList<>();
        int nodeListSize = graph.getNodeAmount();
        for (int i = 0; i < nodeListSize; i++) {
            progressListener.accept((long) i, (long) nodeListSize);
            densityMeasures.add(nodesWithinRadius(i, radius));
        }
        return densityMeasures;
    }

    public int nodesWithinRadius(int source, double radius) {
        int amountScanned = 0;
        List<Double> nodeDist = new ArrayList<>();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            nodeDist.add(Double.MAX_VALUE);
        }
        nodeDist.set(source, 0.0);
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(Comparator.comparing(nodeDist::get));
        priorityQueue.add(source);
        while (!priorityQueue.isEmpty() && (nodeDist.get(priorityQueue.peek()) < radius)) {
            Integer from = priorityQueue.poll();
            amountScanned++;
            for (Edge edge : graph.getAdjList().get(from)) {
                if (nodeDist.get(from) + edge.d < nodeDist.get(edge.to)) {
                    nodeDist.set(edge.to, nodeDist.get(from) + edge.d);
                    priorityQueue.remove(edge.to);
                    priorityQueue.add(edge.to);
                }
            }
        }
        return amountScanned;
    }

    /**
     * @param radius to scan in kilometer from the nodes.
     * @return list of amount of neighbours in radius for all nodes.
     */
    public List<Integer> computeDensityMeasuresReach(double radius, List<Double> reachBounds) {
        List<Integer> densityMeasures = new ArrayList<>();
        int nodeListSize = graph.getNodeAmount();
        for (int i = 0; i < nodeListSize; i++) {
            progressListener.accept((long) i, (long) nodeListSize);
            densityMeasures.add(nodesWithinRadiusReach(i, radius, reachBounds));
        }
        return densityMeasures;
    }

    public int nodesWithinRadiusReach(int source, double radius, List<Double> reachBounds) {
        int amountScanned = 0;
        List<Double> nodeDist = new ArrayList<>();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            nodeDist.add(Double.MAX_VALUE);
        }
        nodeDist.set(source, 0.0);
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(Comparator.comparing(nodeDist::get));
        priorityQueue.add(source);
        while (!priorityQueue.isEmpty() && (nodeDist.get(priorityQueue.peek()) < radius)) {
            Integer from = priorityQueue.poll();
            amountScanned++;
            for (Edge edge : graph.getAdjList().get(from)) {
                double newDist = nodeDist.get(from) + edge.d;
                double reachBound = reachBounds.get(edge.to);
                double projectedDistance = Util.sphericalDistance(graph.getNodeList().get(edge.to), graph.getNodeList().get(getTarget()));
                double precision = 0.00000000000001;
                boolean newDistanceValid = reachBound > newDist || Math.abs(reachBound - newDist) <= precision;
                boolean projectedDistanceValid = reachBound > projectedDistance || Math.abs(reachBound - projectedDistance) <= precision;
                if ((newDistanceValid || projectedDistanceValid) && newDist < nodeDist.get(edge.to)) {
                    nodeDist.set(edge.to, nodeDist.get(from) + edge.d);
                    priorityQueue.remove(edge.to);
                    priorityQueue.add(edge.to);
                }
            }
        }
        return amountScanned;
    }

    public Map<Integer, Integer> SPTWithinRadius(int source, double radius) {
        List<Double> nodeDist = new ArrayList<>();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            nodeDist.add(Double.MAX_VALUE);
        }
        nodeDist.set(source, 0.0);
        Map<Integer, Integer> pathMap = new HashMap<>();
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(Comparator.comparing(nodeDist::get));
        /*for (Node node : graph.getNodeList()) {
            if (node != null) priorityQueue.add(node.index);
        }*/
        priorityQueue.add(source);
        List<Node> nList = graph.getNodeList();
        while (!priorityQueue.isEmpty() && (nodeDist.get(priorityQueue.peek()) < radius)) {
            Integer from = priorityQueue.poll();
            for (Edge edge : graph.getAdjList().get(from)) {
                if (nList.get(edge.to) == null) continue;
                if (nodeDist.get(from) + edge.d < nodeDist.get(edge.to)) {
                    nodeDist.set(edge.to, nodeDist.get(from) + edge.d);
                    priorityQueue.remove(edge.to);
                    priorityQueue.add(edge.to);
                    pathMap.put(edge.to, edge.from);
                }
            }
        }
        return pathMap;
    }

    boolean trace = false;

    private void trace(String msg) {
        if (trace) {
            System.out.print(msg);
        }
    }

    public List<List<Integer>> scc() {
        if (progressListener != null) progressListener.accept(1L, 100L);
        int counter = 0;
        int n = graph.getNodeAmount();
        long pn = 4 * n + n / 100;
        List<List<Edge>> adjList = graph.getAdjList();
        int time = 0;
        Map<Integer, Integer> finishingTimes = new HashMap<>();
        Stack<Integer> whiteNodes = new Stack<>();
        for (int i = n; i > 0; i--) {
            whiteNodes.add(i - 1);
        }
        Stack<Integer> recursionStack = new Stack<>();
        // First DFS
        while (!whiteNodes.isEmpty()) {
            counter++;
            if (progressListener != null) progressListener.accept((long) counter, pn);
            Integer node = whiteNodes.peek();
            if (!recursionStack.isEmpty() && node.equals(recursionStack.peek())) {
                time++;
                whiteNodes.pop();
                recursionStack.pop();
                finishingTimes.put(node, time);
                continue;
            }
            recursionStack.push(node);
            trace("Visited node: " + node);
            for (Edge edge : adjList.get(node)) {
                if (whiteNodes.contains(edge.to) && !recursionStack.contains(edge.to)) {
                    whiteNodes.removeElement(edge.to);
                    whiteNodes.push(edge.to);
                }
            }
            traceStack("First white nodes: ", whiteNodes);
            traceStack("First recursion stack: ", recursionStack);
        }

        List<List<Integer>> sccNodeLists = new ArrayList<>();
        // Reverse edges
        List<List<Edge>> revAdjList = graph.getReverse(adjList);
        // Sort nodes based on finishing time
        List<Node> nodeList = new ArrayList<>(graph.getNodeList());
        nodeList.sort(Comparator.comparing(node -> finishingTimes.get(node.index)));
        for (Node node : nodeList) {
            whiteNodes.add(node.index);
            trace(node.index + " -> " + finishingTimes.get(node.index) + "     ");
        }
        trace("\n");
        recursionStack = new Stack<>();
        // Second DFS
        while (!whiteNodes.isEmpty()) {
            counter++;
            if (progressListener != null) progressListener.accept((long) counter, pn);
            Integer node = whiteNodes.peek();
            if (recursionStack.isEmpty()) {
                sccNodeLists.add(new ArrayList<>());
            } else if (node.equals(recursionStack.peek())) {
                whiteNodes.pop();
                recursionStack.pop();
                int curAdjList = sccNodeLists.size() - 1;
                sccNodeLists.get(curAdjList).add(node);
                continue;
            }
            recursionStack.push(node);
            trace("Visited node 2. pass: " + node + "\n");

            for (Edge edge : revAdjList.get(node)) {
                if (whiteNodes.contains(edge.to) && !recursionStack.contains(edge.to)) {
                    whiteNodes.removeElement(edge.to);
                    whiteNodes.push(edge.to);
                }
            }
            traceStack("First white nodes: ", whiteNodes);
            traceStack("First recursion stack: ", recursionStack);
        }

        // Collect result of GCC in new graphs sorted by size from largest to smallest
        for (List<Integer> sccNodeList : sccNodeLists) {
            trace("{");
            for (Integer node : sccNodeList) {
                trace(node + ", ");
            }
            trace("}\n");
        }
        sccNodeLists.sort((l1, l2) -> l2.size() - l1.size());
        return sccNodeLists;
    }

    public GraphPair subGraphPair(GraphInfo graphInfo, List<Integer> nodeList) {
        Graph subGraph = subGraph(nodeList);
        GraphInfo subGraphInfo = graphInfo != null ? subGraph(graphInfo, nodeList) : null;
        return new GraphPair(subGraph, subGraphInfo);
    }

    public Graph subGraph(List<Integer> nodesToKeep) {
        Graph subGraph = new Graph(nodesToKeep.size());
        subGraph.setSccNodeSet(nodesToKeep);
        Map<Integer, Integer> indexMap = new HashMap<>();
        List<Node> subNodeList = subGraph.getNodeList();
        List<List<Edge>> subAdjList = subGraph.getAdjList();
        for (int i = 0; i < nodesToKeep.size(); i++) {
            indexMap.put(nodesToKeep.get(i), i);
            Node oldNode = graph.getNodeList().get(nodesToKeep.get(i));
            Node newNode = new Node(i, oldNode.longitude, oldNode.latitude);
            subNodeList.add(newNode);
        }

        for (int from : nodesToKeep) {
            for (Edge edge : graph.getAdjList().get(from)) {
                if (nodesToKeep.contains(edge.to)) {
                    int newFrom = indexMap.get(from);
                    int newTo = indexMap.get(edge.to);
                    subAdjList.get(newFrom).add(new Edge(newFrom, newTo, edge.d));
                }
            }
        }
        return subGraph;
    }

    public GraphInfo subGraph(GraphInfo graphInfo, List<Integer> nodesToKeep) {
        GraphInfo subGraph = new GraphInfo(nodesToKeep.size());
        Map<Integer, Integer> indexMap = new HashMap<>();
        List<NodeInfo> subNodeList = subGraph.getNodeList();
        List<List<EdgeInfo>> subAdjList = subGraph.getAdjList();
        for (int i = 0; i < nodesToKeep.size(); i++) {
            indexMap.put(nodesToKeep.get(i), i);
            NodeInfo oldNode = graphInfo.getNodeList().get(nodesToKeep.get(i));
            NodeInfo newNode = new NodeInfo(i, oldNode.getNatureValue(), oldNode.isFuelAmenity());
            subNodeList.add(newNode);
        }

        for (int from : nodesToKeep) {
            for (EdgeInfo edge : graphInfo.getAdjList().get(from)) {
                if (nodesToKeep.contains(edge.getTo())) {
                    int newFrom = indexMap.get(from);
                    int newTo = indexMap.get(edge.getTo());
                    int newMaxSpeed = edge.getMaxSpeed();
                    Surface newSurface = edge.getSurface();
                    subAdjList.get(newFrom).add(new EdgeInfo(newFrom, newTo, newMaxSpeed, newSurface));
                }
            }
        }
        return subGraph;
    }

    public double averageOutDegree() {
        return (double) edgeAmount() / graph.getNodeAmount();
    }

    public long edgeAmount() {
        long c = 0;
        for (List<Edge> edges : graph.getAdjList()) {
            for (Edge edge : edges) {
                c++;
            }
        }
        return c;
    }

    boolean traceStack = false;

    private void traceStack(String s, Stack<Integer> recursionStack) {
        if (!traceStack) {
            return;
        }
        System.out.println(s);
        for (int i = recursionStack.size(); i > max(recursionStack.size() - 10, 0); i--) {
            System.out.print(recursionStack.get(i - 1) + ", ");
        }
        System.out.println();
    }

    public void setProgressListener(BiConsumer<Long, Long> progressListener) {
        this.progressListener = progressListener;
    }
}
