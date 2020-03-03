package paths;

import model.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Dijkstra {

    public static boolean trace = false;

    public static int seed = 0;

    public static PriorityQueue<Integer> nodeQueue;
    public static boolean result = false;

    private static BiFunction<Node, Node, Double> distanceStrategy;

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
        Comparator<Integer> comparator = (i1, i2) -> {
            double doubleD = priorityStrategy.apply(i1) - priorityStrategy.apply(i2);
            if (Math.abs(doubleD) < 0.000009) return i1 - i2;
            if (priorityStrategy.apply(i1) < priorityStrategy.apply(i2)) {
                return -1;
            } else {
                return 1;
            }
        };
        nodeQueue = new PriorityQueue<>(comparator);
        nodeQueue.add(from);
        Set<Integer> seenNodes = new LinkedHashSet<>();
        Map<Integer, Integer> pathMap = new HashMap<>();

        while (!nodeQueue.isEmpty()) {
            Integer currentNode = nodeQueue.poll();
            if (seenNodes.contains(currentNode)) {
                continue;
            }
            seenNodes.add(currentNode);
            if (currentNode == to) {
                break;
            }
            for (Edge edge : adjList.get(currentNode)) {
                relax(nodeDist, pathMap, currentNode, nodeQueue, edge);
                if (trace) {
                    System.out.println("From " + currentNode + " to " + edge.to + " d = " + edge.d);
                }

            }
            trace(nodeQueue); //Print queue if trace
        }

        List<Integer> shortestPath = extractPath(pathMap, adjList, from, to);
        return new ShortestPathResult(nodeDist.get(to), shortestPath, seenNodes.size());
    }

    private static List<Integer> extractPath(Map<Integer, Integer> backPointers, List<List<Edge>> adjList, int from, int to) {
        Integer curNode = to;
        int prevNode = to;
        List<Integer> path = new ArrayList<>(backPointers.size());
        path.add(to);
        while (curNode != from) {
            curNode = backPointers.get(curNode);
            if (curNode == null) {
                return new ArrayList<>(0);
            }
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

        /*List<Integer> path = new ArrayList<>();

        for (Integer i : backPointers) {
            for (Edge edge : adjList.get(i)) {
                if (backPointers.contains(edge.to)) {
                    edge.inPath = true;
                }
            }
            path.add(i);
        }
        return path;*/
    }

    public static ShortestPathResult randomPath(Graph graph, AlgorithmMode mode) {
        int n = graph.getNodeAmount();
        Random random = new Random(seed);
        int from = random.nextInt(n);
        int to = random.nextInt(n);
       /* System.out.println(from);
        System.out.println(to);*/
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
        PriorityQueue<Integer> copy = new PriorityQueue<>(nodeQueue);
        if (trace) {
            System.out.print("Nodequeue: ");
            for (int i = 0; i < copy.size() - 1; i++) {
                Integer object = copy.poll();
                System.out.print(object + " ");
            }
            System.out.println();
        }
    }

    private static void relax(List<Double> nodeDist, Map<Integer, Integer> backPointers, int from, PriorityQueue<Integer> pq, Edge edge) {
        edge.visited = true;
        double newDist = nodeDist.get(from) + edge.d;
        if (newDist < nodeDist.get(edge.to)) {
            pq.remove(edge.to);
            nodeDist.set(edge.to, newDist);
            backPointers.put(edge.to, from);
            pq.add(edge.to);
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

    public static ShortestPathResult bidirectional(Graph graph, int from, int to, AlgorithmMode mode) {
        // TODO: Try to integrate it with sssp Dijkstra implementation.
        // TODO: Bidirectional A_STAR does not return the correct distance.
        // TODO: OutOfMemoryError if no path can be found between from and to
        graph.resetPathTrace();
        List<List<Edge>> adjList = graph.getAdjList();
        List<Double> nodeDistA = initNodeDist(from, adjList.size());
        List<Double> nodeDistB = initNodeDist(to, adjList.size());
        Function<Integer, Double> priorityStrategyA = choosePriorityStrategy(graph, from, to, mode, nodeDistA);
        Comparator<Integer> comparatorA = (i1, i2) -> (int) Math.signum(priorityStrategyA.apply(i1) - priorityStrategyA.apply(i2));
        Function<Integer, Double> priorityStrategyB = choosePriorityStrategy(graph, to, from, mode, nodeDistB);
        Comparator<Integer> comparatorB = (i1, i2) -> (int) Math.signum(priorityStrategyB.apply(i1) - priorityStrategyB.apply(i2));

        // Queue to hold the paths from Node: from.
        PriorityQueue<Integer> queueA = new PriorityQueue<>(comparatorA);
        queueA.add(from);
        // A set of visited nodes starting from Node a.
        Set<Integer> visitedA = new HashSet<>();
        Map<Integer, Integer> backPointersA = new HashMap<>();

        // Queue to hold the paths from Node: to.
        PriorityQueue<Integer> queueB = new PriorityQueue<>(comparatorB);
        queueB.add(to);
        // A set of visited nodes starting from Node b.
        Set<Integer> visitedB = new HashSet<>();
        Map<Integer, Integer> backPointersB = new HashMap<>();

        // Initialize them all to be zero
        /*for (int i = 0; i < graph.getNodeAmount(); i++) {
            backPointersA.add(0);
            backPointersB.add(0);
        }*/

        int middlePoint = 0;
        boolean intersectionFound = false;
        // Both queues need to be empty and an intersection has to be found in order to exit the while loop.
        while (!queueA.isEmpty() && !queueB.isEmpty() && !intersectionFound) {
            // Dijkstra from the 'From'-side
            Integer nextA = queueA.poll();
            if (nextA != null) {
                if (nextA == to) {
                    break;
                }

                for (Edge edge : adjList.get(nextA)) {
                    relax(nodeDistA, backPointersA, nextA, queueA, edge);
                    // If the visited nodes, starting from the other direction,
                    // contain the "adjacent" node of "next", then we can terminate the search
                    if (visitedA.contains(edge.to) && visitedB.contains(edge.to)) {
                        middlePoint = edge.to;
                        intersectionFound = true;
                        break;
                    } else if (!visitedA.contains(edge.to)) {
                        queueA.add(edge.to);
                        visitedA.add(edge.to);
                    }
                }
            }

            // Dijkstra from the 'To'-side
            Integer nextB = queueB.poll();
            if (nextB != null) {
                if (nextB == from) {
                    break;
                }
                for (Edge edge : adjList.get(nextB)) {
                    relax(nodeDistB, backPointersB, nextB, queueB, edge);
                    // If the visited nodes, starting from the other direction,
                    // contain the "adjacent" node of "next", then we can terminate the search
                    if (visitedA.contains(edge.to) && visitedB.contains(edge.to)) {
                        middlePoint = edge.to;
                        intersectionFound = true;
                        break;
                    } else if (!visitedB.contains(edge.to)) {
                        queueB.add(edge.to);
                        visitedB.add(edge.to);
                    }
                }
            }
        }

        visitedA.addAll(visitedB);
        List<Integer> shortestPathA = extractPath(backPointersA, adjList, from, middlePoint);
        List<Integer> shortestPathB = extractPath(backPointersB, adjList, to, middlePoint);
        Collections.reverse(shortestPathB);
        shortestPathA.addAll(shortestPathB);
        double distance = nodeDistA.get(middlePoint) + nodeDistB.get(middlePoint);

        if (trace) {
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
        }

        if (middlePoint != 0) {
            return new ShortestPathResult(distance, shortestPathA, visitedA.size());
        }

        return new ShortestPathResult(Double.MAX_VALUE, new LinkedList<>(), 0);
    }

    public static void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        Dijkstra.distanceStrategy = distanceStrategy;
    }
}
