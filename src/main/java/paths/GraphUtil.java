package paths;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;

import static java.lang.Integer.max;

public class GraphUtil {

    private Graph graph;

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

    public List<Graph> scc() {
        List<List<Edge>> adjList = graph.getAdjList();
        int n = graph.getNodeAmount();
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
            //System.out.println("Visited node: " + node);
            for (Edge edge : adjList.get(node)) {
                if (whiteNodes.contains(edge.to) && !recursionStack.contains(edge.to)) {
                    whiteNodes.removeElement(edge.to);
                    whiteNodes.push(edge.to);
                }
            }
            //printStack("First white nodes: ", whiteNodes);
            //printStack("First recursion stack: ", recursionStack);
        }

        List<List<List<Edge>>> sccAdjLists = new ArrayList<>();
        // Reverse edges
        List<List<Edge>> revAdjList = graph.getReverse(adjList);
        // Sort nodes based on finishing time
        List<Node> nodeList = new ArrayList<>(graph.getNodeList());
        nodeList.sort(Comparator.comparing(node -> finishingTimes.get(node.index)));
        for (Node node : nodeList) {
            System.out.print(node.index + " -> " + finishingTimes.get(node.index) + "     ");
        }
        System.out.println();
        for (int i = 0; i < n; i++) {
            whiteNodes.add(nodeList.get(i).index);
        }
        // Second DFS
        Stack<Integer> tree = new Stack<>();
        sccAdjLists.add(new ArrayList<>());
        while (!whiteNodes.isEmpty()) {
            Integer node = whiteNodes.pop();

            for (Edge edge : revAdjList.get(node)) {
                if (whiteNodes.contains(edge.to)) {
                    whiteNodes.remove(edge.to);
                    whiteNodes.push(edge.to);
                }
            }
            time++;
        }

        List<Graph> graphs = new ArrayList<>();
        graphs.add(new Graph(0));

        return null;
    }

    private void printStack(String s, Stack<Integer> recursionStack) {
        System.out.print(s);
        for (int i = recursionStack.size(); i > max(recursionStack.size() - 10, 0); i--) {
            System.out.print(recursionStack.get(i - 1) + ", ");
        }
        System.out.println();
    }
}
