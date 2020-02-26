package paths;

import model.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Dijkstra {

    public static boolean trace = false;

    public static int seed = 0;

    public static boolean result = false;

    public static BiFunction<Node, Node, Double> distanceStrategy = Util::sphericalDistance;

    private static Function<Integer, Double> priorityStrategy;

    private static Function<Integer, Double> choosePriorityStrategy(Graph graph, int from, int to, AlgorithmMode mode, List<Double> nodeDist) {
        List<Node> nodeList = graph.getNodeList();
        switch (mode) {
            default:
            case DIJKSTRA:
                return nodeDist::get;
            case A_STAR_DIST:
                return (i) -> {
                    Node curNode = nodeList.get(i);
                    Node target = nodeList.get(to);
                    return nodeDist.get(i) + distanceStrategy.apply(curNode, target);
                };
        }
    }

    public static ShortestPathResult sssp(Graph graph, int from, int to, AlgorithmMode mode) {
        graph.resetPathTrace();
        List<List<Edge>> adjList = graph.getAdjList();
        List<Double> nodeDist = initNodeDist(from, adjList.size());
        priorityStrategy = choosePriorityStrategy(graph, from, to, mode, nodeDist);
        Comparator<Integer> comparator = (i1, i2) -> (int) Math.signum(priorityStrategy.apply(i1) - priorityStrategy.apply(i2));
        PriorityQueue<Integer> nodeQueue = new PriorityQueue<>(comparator);
        nodeQueue.add(from);
        Set<Integer> seenNodes = new HashSet<>();
        List<Integer> backPointers = new ArrayList<>();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
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

        List<Integer> shortestPath = extractPath(backPointers, adjList, from, to);
        return new ShortestPathResult(nodeDist.get(to), shortestPath, seenNodes.size());
    }

    private static List<Integer> extractPath(List<Integer> backPointers, List<List<Edge>> adjList, int from, int to) {
        int curNode = to;
        int prevNode = to;
        List<Integer> path = new ArrayList<>(to);
        while (curNode != from) {
            curNode = backPointers.get(curNode);
            // System.out.println(curNode);
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
        int n = graph.getNodeAmount();
        Random random = new Random(seed);
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

    private static void relax(List<Double> nodeDist, List<Integer> backPointers, int from, Edge edge) {
        edge.visited = true;
        double newDist = nodeDist.get(from) + edge.d;
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
    private static List<Double> initNodeDist(int from, int size) {
        List<Double> nodeDist = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            nodeDist.add(Double.MAX_VALUE);
        }
        nodeDist.set(from, 0.0);
        return nodeDist;
    }

    /*
     * Returns true if a path exists between Node a and b, false otherwise.
     * */
    public static ShortestPathResult bidirectional(Graph graph, int from, int to, AlgorithmMode mode) {
        System.out.println("Started running Bidirectional");
        graph.resetPathTrace();
        List<List<Edge>> adjList = graph.getAdjList();
        List<Double> nodeDist = initNodeDist(from, adjList.size());
        priorityStrategy = choosePriorityStrategy(graph, from, to, mode, nodeDist);
        Comparator<Integer> comparator = (i1, i2) -> (int) Math.signum(priorityStrategy.apply(i1) - priorityStrategy.apply(i2));

        // Queue to hold the paths from Node: from.
        PriorityQueue<Integer> queueFrom = new PriorityQueue<>(comparator);
        queueFrom.add(from);

        // Queue to hold the paths from Node: to.
        PriorityQueue<Integer> queueTo = new PriorityQueue<>(comparator);
        queueTo.add(to);

        // A set of visited nodes starting from Node a.
        Set<Integer> visitedA = new HashSet<>();
        List<Integer> backPointersA = new ArrayList<>();

        // A set of visited nodes starting from Node b.
        Set<Integer> visitedB = new HashSet<>();
        List<Integer> backPointersB = new ArrayList<>();

        // Initialize them all to be zero
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            backPointersA.add(0);
            backPointersB.add(0);
        }

        int middlePoint = 0;
        // Both queues need to be empty to exit the while loop.
        while (!queueFrom.isEmpty() && !queueTo.isEmpty()) {
            int nextFrom = queueFrom.poll();
            if (nextFrom == to) {
                break;
            }

            for (Edge edge : adjList.get(nextFrom)) {
                relax(nodeDist, backPointersA, nextFrom, edge);
                // If the visited nodes, starting from the other direction,
                // contain the "adjacent" node of "next", then we can terminate the search
                if (visitedB.contains(edge.to)) {
                    middlePoint = edge.to;
                    System.out.println("Route found from A to B");
                    return getShortestPathResult(from, to, adjList, nodeDist, visitedA, backPointersA, visitedB, backPointersB, middlePoint);
                } else if (visitedA.add(edge.to)) {
                    queueFrom.add(edge.to);

                }
            }

            int nextTo = queueTo.poll();
            if (nextTo == from) {
                break;
            }

            for (Edge edge : adjList.get(nextTo)) {
                relax(nodeDist, backPointersB, nextTo, edge);
                // If the visited nodes, starting from the other direction,
                // contain the "adjacent" node of "next", then we can terminate the search
                if (visitedA.contains(edge.to)) {
                    middlePoint = edge.to;
                    System.out.println("Route found from B to A");
                    return getShortestPathResult(from, to, adjList, nodeDist, visitedA, backPointersA, visitedB, backPointersB, middlePoint);
                } else if (visitedB.add(edge.to)){
                    queueTo.add(edge.to);
                }
            }
        }

        return getShortestPathResult(from, to, adjList, nodeDist, visitedA, backPointersA, visitedB, backPointersB, middlePoint);
    }

    private static ShortestPathResult getShortestPathResult(int from, int to, List<List<Edge>> adjList, List<Double> nodeDist, Set<Integer> visitedA, List<Integer> backPointersA, Set<Integer> visitedB, List<Integer> backPointersB, Integer middlePoint) {
//        List<Integer> mergedList = new ArrayList<>();
//        for (int i = 0; i < backPointersA.size(); i++) {
//            mergedList.add(0);
//        }
//
//        for (int i = 0; i < backPointersA.size(); i++) {
//            if (backPointersA.get(i) <= backPointersB.get(i)) {
//                mergedList.set(i, backPointersB.get(i));
//            } else {
//                mergedList.set(i, backPointersA.get(i));
//            }
//        }
//
//        System.out.println("MergedList");
//        System.out.println(mergedList);
//        System.out.println(mergedList.size());
//        System.out.println();

        visitedA.addAll(visitedB);
        System.out.println(visitedA);
        System.out.println(visitedA.size());
        System.out.println();

        List<Integer> shortestPathA = extractPath(backPointersA, adjList, from, middlePoint);
        List<Integer> shortestPathB = extractPath(backPointersB, adjList, to, middlePoint);
        Collections.reverse(shortestPathB);

        System.out.println(shortestPathA);
        System.out.println(shortestPathB);

        shortestPathA.addAll(shortestPathB);

        return new ShortestPathResult(nodeDist.get(to), shortestPathA, visitedA.size());
    }
}
