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
        // First DFS
        while (!whiteNodes.isEmpty()) {
            Integer node = whiteNodes.pop();
            //System.out.println("Visited node: " + node);
            for (Edge edge : adjList.get(node)) {
                if (whiteNodes.contains(edge.to)) {
                    whiteNodes.removeElement(edge.to);
                    whiteNodes.push(edge.to);
                }
            }
            /*System.out.print("First white nodes: ");
            for (int i = whiteNodes.size(); i > max(whiteNodes.size() - 10, 0); i--) {
                System.out.print(whiteNodes.get(i - 1) + ", ");
            }
            System.out.println();*/
            time++;
            finishingTimes.put(node, time);
        }

        List<List<List<Edge>>> sccAdjLists = new ArrayList<>();
        // Reverse edges
        adjList = graph.getReverse(adjList);
        // Sort nodes based on finishing time
        List<Node> nodeList = graph.getNodeList();
        nodeList.sort(Comparator.comparing(node -> finishingTimes.get(node.index)));
        for (int i = 0; i < n; i++) {
            whiteNodes.add(nodeList.get(i).index);
        }
        // Second DFS
        /*Stack<Integer> tree = new Stack<>();
        sccAdjLists.add(new ArrayList<>());
        while (!whiteNodes.isEmpty()) {
            Integer node = whiteNodes.pop();

            for (Edge edge : adjList.get(node)) {
                if (whiteNodes.contains(edge.to)) {
                    whiteNodes.remove(edge.to);
                    whiteNodes.push(edge.to);
                }
            }
            time++;
        }

        List<Graph> graphs = new ArrayList<>();
        graphs.add(new Graph(0));*/

        return null;
    }
}
