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
            case DIJKSTRA:
            case BI_DIJKSTRA:
                return nodeDist::get;
            case A_STAR:
            case BI_A_STAR:
                return (i) -> {
                    Node curNode = nodeList.get(i);
                    Node target = nodeList.get(to);
                    return nodeDist.get(i) + distanceStrategy.apply(curNode, target);
                };
        }
        return null;
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

        ShortestPathResult res;
        if (mode == AlgorithmMode.DIJKSTRA || mode == AlgorithmMode.A_STAR) {
            res = sssp(graph, from, to, mode);
        } else {
            res = bidirectional(graph, from, to, mode);
        }

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
        // TODO: Bidirectional A_STAR does not return the correct distance.
        graph.resetPathTrace();
        List<List<Edge>> adjListA = graph.getAdjList();
        List<List<Edge>> adjListB = graph.getAdjList();
        List<Double> nodeDistA = initNodeDist(from, adjListA.size());
        List<Double> nodeDistB = initNodeDist(to, adjListB.size());
        Function<Integer, Double> priorityStrategyA = choosePriorityStrategy(graph, from, to, mode, nodeDistA);
        Comparator<Integer> comparatorA = (i1, i2) -> (int) Math.signum(priorityStrategyA.apply(i1) - priorityStrategyA.apply(i2));
        Function<Integer, Double> priorityStrategyB = choosePriorityStrategy(graph, to, from, mode, nodeDistB);
        Comparator<Integer> comparatorB = (i1, i2) -> (int) Math.signum(priorityStrategyB.apply(i1) - priorityStrategyB.apply(i2));


        // Queue to hold the paths from Node: from.
        PriorityQueue<Integer> queueA = new PriorityQueue<>(comparatorA);
        queueA.add(from);
        // A set of visited nodes starting from Node a.
        Set<Integer> visitedA = new HashSet<>();
        List<Integer> backPointersA = new ArrayList<>();

        // Queue to hold the paths from Node: to.
        PriorityQueue<Integer> queueB = new PriorityQueue<>(comparatorB);
        queueB.add(to);
        // A set of visited nodes starting from Node b.
        Set<Integer> visitedB = new HashSet<>();
        List<Integer> backPointersB = new ArrayList<>();

        // Initialize them all to be zero
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            backPointersA.add(0);
            backPointersB.add(0);
        }

        int middlePoint = 0;
        boolean intersectionFound = false;
        // Both queues need to be empty and an intersection has to be found in order to exit the while loop.
        while (!queueA.isEmpty() && !queueB.isEmpty() && !intersectionFound) {
            int nextA = queueA.poll();
            if (nextA == to) {
                break;
            }

            for (Edge edge : adjListA.get(nextA)) {
                relax(nodeDistA, backPointersA, nextA, edge);
                // If the visited nodes, starting from the other direction,
                // contain the "adjacent" node of "next", then we can terminate the search
                if (visitedA.contains(edge.to) && visitedB.contains(edge.to)) {
                    middlePoint = edge.to;
                    System.out.println("Route found from A to B");
                    intersectionFound = true;
                    break;
                } else if (!visitedA.contains(edge.to)) {
                    queueA.add(edge.to);
                    visitedA.add(edge.to);
                }
            }

            int nextB = queueB.poll();
            if (nextB == from) {
                break;
            }

            for (Edge edge : adjListB.get(nextB)) {
                relax(nodeDistB, backPointersB, nextB, edge);
                // If the visited nodes, starting from the other direction,
                // contain the "adjacent" node of "next", then we can terminate the search
                if (visitedA.contains(edge.to) && visitedB.contains(edge.to)) {
                    middlePoint = edge.to;
                    System.out.println("Route found from B to A");
                    intersectionFound = true;
                    break;
                } else if (!visitedB.contains(edge.to)) {
                    queueB.add(edge.to);
                    visitedB.add(edge.to);
                }
            }
        }

        visitedA.addAll(visitedB);
        System.out.println(visitedA);
        System.out.println(visitedA.size());
        System.out.println();

        System.out.println("From: " + from);
        System.out.println("MiddlePoint: " + middlePoint);
        System.out.println("To: " + to);

        System.out.println("Contains MiddlePoint A: " + visitedA.contains(middlePoint));
        System.out.println("Contains MiddlePoint B: " + visitedB.contains(middlePoint));

        System.out.println("Get MiddlePoint A: " + backPointersA.get(middlePoint));
        System.out.println("Get MiddlePoint B: " + backPointersB.get(middlePoint));

        List<Integer> shortestPathA = extractPath(backPointersA, adjListA, from, middlePoint);
        System.out.println(shortestPathA);

        List<Integer> shortestPathB = extractPath(backPointersB, adjListB, to, middlePoint);
        System.out.println(shortestPathB);
        Collections.reverse(shortestPathB);
        shortestPathA.addAll(shortestPathB);

        return new ShortestPathResult(nodeDistA.get(middlePoint) + nodeDistB.get(middlePoint), shortestPathA, visitedA.size());
    }
}
