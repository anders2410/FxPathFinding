package paths;

import model.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.singletonList;

public class Dijkstra {

    public static boolean trace = false;

    public static boolean traceResult = false;

    public static int seed = 0;

    private static List<Node> nodeList;

    private static BiFunction<Node, Node, Double> distanceStrategy;

    private static Graph graph;

    private static int source, target;

    private static AlgorithmMode mode;

    private static Function<Integer, Double> choosePriorityStrategy(List<Double> nodeDist) {
        List<Node> nodeList = graph.getNodeList();
        switch (mode) {
            default:
            case DIJKSTRA:
            case BI_DIJKSTRA:
                return nodeDist::get;
            case A_STAR:
            case BI_A_STAR:
                return (i) -> {
                    Node curNode = nodeList.get(i);
                    Node targetNode = nodeList.get(target);
                    return nodeDist.get(i) + distanceStrategy.apply(curNode, targetNode);
                };
        }
    }

    private static RelaxStrategy chooseRelaxStrategy(List<Double> nodeDist, Map<Integer, Double> estimatedDist, Map<Integer, Integer> pathMap, AbstractQueue<Integer> pq) {
        switch (mode) {
            default:
            case DIJKSTRA:
            case BI_DIJKSTRA:
                return (from, edge) -> {
                    edge.visited = true;
                    double newDist = nodeDist.get(from) + edge.d;

                    if (newDist < nodeDist.get(edge.to)) {
                        pq.remove(edge.to);
                        nodeDist.set(edge.to, newDist);
                        pathMap.put(edge.to, from);
                        pq.add(edge.to);
                    }
                };
            case A_STAR:
            case BI_A_STAR:
                return (from, edge) -> {
                    edge.visited = true;
                    double newDist = nodeDist.get(from) + edge.d;
                    double weirdWeight = estimatedDist.get(from) + edge.d - distanceStrategy.apply(nodeList.get(from), nodeList.get(target)) + distanceStrategy.apply(nodeList.get(edge.to), nodeList.get(target));
                    if (weirdWeight < estimatedDist.getOrDefault(edge.to, Double.MAX_VALUE)) {
                        pq.remove(edge.to);
                        estimatedDist.put(edge.to, weirdWeight);
                        nodeDist.set(edge.to, newDist);
                        pathMap.put(edge.to, from);
                        pq.add(edge.to);
                    }
                };
        }
    }

    private static void initializeGlobalFields(Graph graphP, AlgorithmMode modeP, int sourceP, int targetP) {
        graph = graphP;
        graph.resetPathTrace();
        nodeList = graph.getNodeList();
        mode = modeP;
        source = sourceP;
        target = targetP;
    }

    public static ShortestPathResult sssp(Graph graphP, int sourceP, int targetP, AlgorithmMode modeP) {
        initializeGlobalFields(graphP, modeP, sourceP, targetP);
        if (source == target) {
            return new ShortestPathResult(0, singletonList(source), 0);
        }
        ShortestPathResult result;
        if (mode == AlgorithmMode.BI_DIJKSTRA || mode == AlgorithmMode.BI_A_STAR) {
            result = biDirectional();
        } else {
            result = oneDirectional();
        }
        return result;
    }

    private static ShortestPathResult oneDirectional() {
        List<List<Edge>> adjList = graph.getAdjList();
        List<Double> nodeDist = initNodeDist(source, adjList.size());
        Map<Integer, Double> estimatedDist = null;
        if (mode == AlgorithmMode.A_STAR) {
            estimatedDist = new HashMap<>();
            estimatedDist.put(source, 0.0);
        }
        Function<Integer, Double> priorityStrategy = choosePriorityStrategy(nodeDist);
        Comparator<Integer> comparator = Comparator.comparingDouble(priorityStrategy::apply);
        AbstractQueue<Integer> nodeQueue = new PriorityQueue<>(comparator);
        nodeQueue.add(source);
        Set<Integer> seenNodes = new LinkedHashSet<>();
        Map<Integer, Integer> pathMap = new HashMap<>();
        RelaxStrategy relaxStrategy = chooseRelaxStrategy(nodeDist, estimatedDist, pathMap, nodeQueue);

        while (!nodeQueue.isEmpty()) {
            Integer currentNode = nodeQueue.poll();
            if (seenNodes.contains(currentNode)) {
                continue;
            }
            seenNodes.add(currentNode);
            if (currentNode == target) {
                break;
            }
            for (Edge edge : adjList.get(currentNode)) {
                relaxStrategy.relax(currentNode, edge);
                if (trace) {
                    System.out.println("From " + currentNode + " to " + edge.to + " d = " + edge.d);
                }
            }
            trace(nodeQueue); //Print queue if trace
        }

        List<Integer> shortestPath = extractPath(pathMap, adjList, source, target);
        return new ShortestPathResult(nodeDist.get(target), shortestPath, seenNodes.size());
    }

    private static ShortestPathResult biDirectional() {
        // Implementation pseudocode from https://www.cs.princeton.edu/courses/archive/spr06/cos423/Handouts/EPP%20shortest%20path%20algorithms.pdf
        // TODO: Try to integrate it with sssp Dijkstra implementation.
        List<List<Edge>> adjList = graph.getAdjList();
        List<List<Edge>> revAdjList = graph.reverseAdjacencyList(adjList);

        //A
        List<Double> nodeDistA = initNodeDist(source, adjList.size());
        Map<Integer, Double> estimatedDistA = null;
        if (mode == AlgorithmMode.BI_A_STAR) {
            estimatedDistA = new HashMap<>();
            estimatedDistA.put(source, 0.0);
        }
        Function<Integer, Double> priorityStrategyA = choosePriorityStrategy(nodeDistA);
        Comparator<Integer> comparatorA = Comparator.comparingDouble(priorityStrategyA::apply);

        // Queue to hold the paths from Node: source.
        PriorityQueue<Integer> queueA = new PriorityQueue<>(comparatorA);
        queueA.add(source);
        // A set of visited nodes starting from Node a.
        Set<Integer> visitedA = new HashSet<>();
        Map<Integer, Integer> pathMapA = new HashMap<>();
        RelaxStrategy relaxStrategyA = chooseRelaxStrategy(nodeDistA, estimatedDistA, pathMapA, queueA);

        //B
        List<Double> nodeDistB = initNodeDist(target, adjList.size());
        Map<Integer, Double> estimatedDistB = null;
        if (mode == AlgorithmMode.BI_A_STAR) {
            estimatedDistB = new HashMap<>();
            estimatedDistB.put(target, 0.0);
        }
        Function<Integer, Double> priorityStrategyB = choosePriorityStrategy(nodeDistB);
        Comparator<Integer> comparatorB = Comparator.comparingDouble(priorityStrategyB::apply);

        // Queue to hold the paths from Node: to.
        PriorityQueue<Integer> queueB = new PriorityQueue<>(comparatorB);
        queueB.add(target);
        // A set of visited nodes starting from Node b.
        Set<Integer> visitedB = new HashSet<>();
        Map<Integer, Integer> pathMapB = new HashMap<>();
        RelaxStrategy relaxStrategyB = chooseRelaxStrategy(nodeDistB, estimatedDistB, pathMapB, queueB);

        double goalDistance = Double.MAX_VALUE;
        boolean intersectionFound = false;
        int middlePoint = -1;
        // Both queues need to be empty and an intersection has to be found in order to exit the while loop.
        while (!queueA.isEmpty() && !queueB.isEmpty()) {
            // Dijkstra from the 'From'-side
            if (checkTermination(nodeDistA, queueA, nodeDistB, queueB, goalDistance)) break;
            Integer nextA = queueA.poll();
            if (nextA != null) {
                visitedA.add(nextA);
                for (Edge edge : adjList.get(nextA)) {
                    if (visitedB.contains(edge.to)) {
                        if (nodeDistA.get(nextA) + edge.d + nodeDistB.get(edge.to) < goalDistance) {
                            middlePoint = edge.to;
                            goalDistance = nodeDistA.get(nextA) + edge.d + nodeDistB.get(edge.to);
                        }
                    }
                    relaxStrategyA.relax(nextA, edge);
                }
            }

            // Dijkstra from the 'To'-side
            if (checkTermination(nodeDistA, queueA, nodeDistB, queueB, goalDistance)) break;
            Integer nextB = queueB.poll();
            if (nextB != null) {
                visitedB.add(nextB);
                for (Edge edge : revAdjList.get(nextB)) {
                    if (visitedA.contains(edge.to)) {
                        if (nodeDistB.get(nextB) + edge.d + nodeDistA.get(edge.to) < goalDistance) {
                            middlePoint = edge.to;
                            goalDistance = nodeDistB.get(nextB) + edge.d + nodeDistA.get(edge.to);
                        }
                    }
                    relaxStrategyB.relax(nextB, edge);
                }
            }
        }

        visitedA.addAll(visitedB);
        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, middlePoint);
        List<Integer> shortestPathB = extractPath(pathMapB, revAdjList, target, middlePoint);
        graph.reversePaintEdges(revAdjList, adjList);

        if (middlePoint == -1) {
            return new ShortestPathResult(Double.MAX_VALUE, new LinkedList<>(), 0);
        }
        shortestPathB.remove(shortestPathB.size() - 1);
        Collections.reverse(shortestPathB);
        shortestPath.addAll(shortestPathB);
        double distance = goalDistance;
        visitedA.addAll(visitedB);
        return new ShortestPathResult(distance, shortestPath, visitedA.size());
    }

    private static boolean checkTermination(List<Double> nodeDistA, PriorityQueue<Integer> queueA, List<Double> nodeDistB, PriorityQueue<Integer> queueB, double goalDistance) {
        Integer topA = queueA.peek();
        Integer topB = queueB.peek();
        if (topA != null && topB != null) {
            double combinedDistance = nodeDistA.get(topA) + nodeDistB.get(topB);
            return combinedDistance >= goalDistance;
        }
        return false;
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
    }

    public static ShortestPathResult randomPath(Graph graphP, AlgorithmMode modeP) {
        int n = graphP.getNodeAmount();
        Random random = new Random(seed);
        int sourceR = random.nextInt(n);
        int targetR = random.nextInt(n);
        initializeGlobalFields(graphP, modeP, sourceR, targetR);
        ShortestPathResult res;
        res = sssp(graph, source, target, mode);
        if (traceResult) {
            System.out.println("Distance from " + source + " to " + target + " is " + res.d);
            System.out.println("Graph has " + n + " nodes.");
        }
        return res;
    }

    private static void trace(AbstractQueue<Integer> nodeQueue) {
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

    /**
     * @param from from
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

    private static void printInfo(Set<Integer> visitedA, Map<Integer, Integer> backPointersA, Set<Integer> visitedB, Map<Integer, Integer> backPointersB, int middlePoint) {
        System.out.println(visitedA);
        System.out.println(visitedA.size());
        System.out.println();

        System.out.println("From: " + source);
        System.out.println("MiddlePoint: " + middlePoint);
        System.out.println("To: " + target);

        System.out.println("Contains MiddlePoint A: " + visitedA.contains(middlePoint));
        System.out.println("Contains MiddlePoint B: " + visitedB.contains(middlePoint));

        System.out.println("Get MiddlePoint A: " + backPointersA.get(middlePoint));
        System.out.println("Get MiddlePoint B: " + backPointersB.get(middlePoint));
    }

    public static void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        Dijkstra.distanceStrategy = distanceStrategy;
    }
}
