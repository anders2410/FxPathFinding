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

        List<List<Integer>> sccNodeLists = new ArrayList<>();
        // Reverse edges
        List<List<Edge>> revAdjList = graph.getReverse(adjList);
        // Sort nodes based on finishing time
        List<Node> nodeList = new ArrayList<>(graph.getNodeList());
        nodeList.sort(Comparator.comparing(node -> finishingTimes.get(node.index)));
        for (Node node : nodeList) {
            whiteNodes.add(node.index);
            System.out.print(node.index + " -> " + finishingTimes.get(node.index) + "     ");
        } System.out.println();
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
            //System.out.println("Visited node 2. pass: " + node);

            for (Edge edge : revAdjList.get(node)) {
                if (whiteNodes.contains(edge.to) && !recursionStack.contains(edge.to)) {
                    whiteNodes.removeElement(edge.to);
                    whiteNodes.push(edge.to);
                }
            }
            //printStack("First white nodes: ", whiteNodes);
            //printStack("First recursion stack: ", recursionStack);
        }


        // Collect result of GCC in new graphs

        for (List<Integer> sccNodeList : sccNodeLists) {
            System.out.print("{");
            for (Integer integer : sccNodeList) {
                System.out.print(integer + ", ");
            }
            System.out.println("}");
        }

        List<Graph> graphs = new ArrayList<>();
        for (List<Integer> sccNodeList : sccNodeLists) {
            Graph sccGraph = new Graph(sccNodeList.size());
            for (Integer node_id : sccNodeList) {
                sccGraph.addNode(nodeList.get(node_id));
            }
        }
        return graphs;
    }

    private void printStack(String s, Stack<Integer> recursionStack) {
        System.out.print(s);
        for (int i = recursionStack.size(); i > max(recursionStack.size() - 10, 0); i--) {
            System.out.print(recursionStack.get(i - 1) + ", ");
        }
        System.out.println();
    }
}
