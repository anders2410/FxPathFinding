package paths;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.lang.Integer.max;

public class GraphUtil {

    private Graph graph;

    private BiConsumer<Long, Long> progressListener = (l1, l2) -> {};

    public GraphUtil(Graph graph) {
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

    boolean trace = false;
    private void trace(String msg) {
        if (trace) {
            System.out.print(msg);
        }
    }

    public List<Graph> scc() {
        int n = graph.getNodeAmount();
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
        progressListener.accept(33L, 100L);

        List<List<Integer>> sccNodeLists = new ArrayList<>();
        // Reverse edges
        List<List<Edge>> revAdjList = graph.getReverse(adjList);
        // Sort nodes based on finishing time
        List<Node> nodeList = new ArrayList<>(graph.getNodeList());
        nodeList.sort(Comparator.comparing(node -> finishingTimes.get(node.index)));
        for (Node node : nodeList) {
            whiteNodes.add(node.index);
            trace(node.index + " -> " + finishingTimes.get(node.index) + "     ");
        } trace("\n");
        recursionStack = new Stack<>();
        // Second DFS
        while (!whiteNodes.isEmpty()) {
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
            } trace("}\n");
        }

        progressListener.accept(66L, 100L);
        Comparator<Graph> graphComp = (g1, g2) -> Integer.compare(g2.getNodeAmount(), g1.getNodeAmount());
        return sccNodeLists.stream().map(this::subGraph).sorted(graphComp).collect(Collectors.toList());
    }

    public Graph subGraph(List<Integer> nodesToKeep) {
        Graph subGraph = new Graph(nodesToKeep.size());
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
                    subAdjList.get(newFrom).add(new Edge(newTo, edge.d));
                }
            }
        }
        return subGraph;
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

    // Calculates the incoming nodes
    public Map<Integer, List<Node>> getInDegreeNodeMap() {
        Map<Integer, List<Node>> map = new HashMap<>();
        List<Node> nodeList = graph.getNodeList();

        for (int i = 0; i < nodeList.size(); i++) {
            for (Edge e : graph.getAdjList().get(i)) {
                List<Node> list = map.computeIfAbsent(e.to, k -> new ArrayList<>());
                list.add(nodeList.get(i));
                map.replace(e.to, list);
            }
        }

        return map;
    }

    // Calculate the outgoing degree
    public int getOutDegree(int n) {
        return graph.getAdjList().get(n).size();
    }
}
