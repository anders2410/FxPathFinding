package paths;

import model.*;

import java.util.*;
import java.util.function.Function;

public class Dijkstra {

    public static boolean trace = false;

    public static boolean result = false;

    private static Function<Integer, Float> priorityStrategy;

    private static Function<Integer, Float> choosePriorityStrategy(Graph graph, int from, int to, AlgorithmMode mode, List<Float> nodeDist) {
        List<Node> nodeList = graph.getNodeList();
        switch (mode) {
            default: case DIJKSTRA:
                return nodeDist::get;
            case A_STAR_DIST:
                return (i) -> {
                    Node curNode = nodeList.get(i);
                    Node target = nodeList.get(to);
                    return nodeDist.get(i) + Util.getNodeDistance(curNode, target);
                };
        }
    }

    public static ShortestPathResult sssp(Graph graph, int from, int to, AlgorithmMode mode) {
        graph.resetPathTrace();
        List<List<Edge>> adjList = graph.getAdjList();
        List<Float> nodeDist = initNodeDist(from, adjList.size());
        priorityStrategy = choosePriorityStrategy(graph, from, to, mode, nodeDist);
        Comparator<Integer> comparator = (i1, i2) -> (int) (priorityStrategy.apply(i1) - priorityStrategy.apply(i2));
        PriorityQueue<Integer> nodeQueue = new PriorityQueue<>(comparator);
        nodeQueue.add(from);
        Set<Integer> seenNodes = new HashSet<>();
        List<Integer> backPointers = new ArrayList<>();
        for (int i = 0; i < graph.getNodeSize(); i++) {
            backPointers.add(0);
        }

        while (!nodeQueue.isEmpty()) {
            int currentNode = nodeQueue.poll();
            if (currentNode == to) {
                break;
            }
            for (Edge edge : adjList.get(currentNode)) {
                relax(nodeDist, backPointers, currentNode, edge);
                if (!seenNodes.contains(edge.to)) {
                    nodeQueue.add(edge.to);
                    seenNodes.add(edge.to);
                    if (trace) {
                        System.out.println("From " + currentNode + " to " + edge.to + " d = " + edge.d);
                    }
                }
            }
            trace(nodeQueue); //Print queue if trace
        }
        if (result) {
            System.out.println("Number of visited nodes: " + seenNodes.size());
        }

        List<Integer> shortestPath = extractPath(backPointers, adjList, from, to);
        return new ShortestPathResult(nodeDist.get(to), shortestPath);
    }

    private static List<Integer> extractPath(List<Integer> backPointers, List<List<Edge>> adjList, int from, int to) {
        int curNode = to;
        int prevNode = to;
        List<Integer> path = new ArrayList<>(to);
        while (curNode != from) {
            curNode = backPointers.get(curNode);
            path.add(curNode);
            for (Edge edge : adjList.get(curNode)) {
                if (edge.to == prevNode) {
                    edge.inPath = true;
                }
            }
            prevNode = curNode;
        }
        Collections.reverse(path);
        return path;
    }

    public static ShortestPathResult randomPath(Graph graph, AlgorithmMode mode) {
        int n = graph.getNodeSize();
        Random random = new Random();
        random.setSeed(120471);
        int from = random.nextInt(n);
        int to = random.nextInt(n);
        ShortestPathResult res = sssp(graph, from, to, mode);
        if (result) {
            System.out.println("Distance from " + from + " to " + to + " is " + res.d);
            System.out.println("Graph has " + n + " nodes.");
        }
        return res;
    }

    private static void trace(PriorityQueue<Integer> nodeQueue) {
        if (trace) {
            System.out.print("NodeQueue: ");
            for (Integer integer : nodeQueue) {
                System.out.print(integer + " ");
            }
            System.out.println();
        }
    }

    private static void relax(List<Float> nodeDist, List<Integer> backPointers, int from, Edge edge) {
        edge.visited = true;
        float newDist = nodeDist.get(from) + edge.d;
        if (newDist < nodeDist.get(edge.to)) {
            nodeDist.set(edge.to, newDist);
            backPointers.set(edge.to, from);
        }
    }

    /**
     * @param from id of source node
     * @param size number of nodes in total
     * @return a list of integers with max values in every entry, but that of the source node which is 0.
     */
    private static List<Float> initNodeDist(int from, int size) {
        List<Float> nodeDist = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            nodeDist.add(Float.MAX_VALUE);
        }
        nodeDist.set(from, 0f);
        return nodeDist;
    }
}
