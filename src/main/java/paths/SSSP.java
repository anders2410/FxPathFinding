package paths;

import datastructures.MinPriorityQueue;
import model.*;
import paths.factory.*;
import paths.strategy.*;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Collections.singletonList;
import static paths.AlgorithmMode.*;
import static paths.ABDir.*;

public class SSSP {

    public static boolean trace = false;
    public static boolean traceResult = false;
    public static int seed = 0;

    private static Graph graph;
    private static int source, target;
    private static AlgorithmMode mode;
    private static double goalDistance;
    private static int middlePoint;
    private static double[][] landmarkArray;

    private static boolean biDirectional;
    private static BiFunction<Node, Node, Double> distanceStrategy;
    private static HeuristicFunction heuristicFunction;
    private static TerminationStrategy terminationStrategy;

    private static RelaxStrategy relaxStrategyA;
    private static RelaxStrategy relaxStrategyB;

    private static PriorityStrategy priorityStrategyA;
    private static PriorityStrategy priorityStrategyB;

    private static List<Double> nodeDistA;
    private static List<Double> nodeDistB;
    private static Set<Integer> visitedA;                   // A set of visited nodes starting from Node: source
    private static Set<Integer> visitedB;                   // A set of visited nodes starting from Node: target
    private static Map<Integer, Integer> pathMapA;
    private static Map<Integer, Integer> pathMapB;
    private static MinPriorityQueue queueA;           // Queue to hold the paths from Node: source
    private static MinPriorityQueue queueB;           // Queue to hold the paths from Node: target
    private static Map<Integer, Double> estimatedDistA;
    private static Map<Integer, Double> estimatedDistB;
    private static GetPQueueStrategy priorityQueueGetter;

    // Initialization

    private static void initFields(AlgorithmMode modeP, int sourceP, int targetP) {
        mode = modeP;
        source = sourceP;
        target = targetP;
    }

    private static void initDataStructures() {
        nodeDistA = initNodeDist(source, graph.getNodeAmount());
        nodeDistB = initNodeDist(target, graph.getNodeAmount());
        visitedA = new HashSet<>();
        visitedB = new HashSet<>();
        pathMapA = new HashMap<>();
        pathMapB = new HashMap<>();
        queueA = priorityQueueGetter.initialiseNewQueue(getComparator(priorityStrategyA, A), graph.getNodeAmount());
        queueB = priorityQueueGetter.initialiseNewQueue(getComparator(priorityStrategyB, B), graph.getNodeAmount());
        estimatedDistA = new HashMap<>();
        estimatedDistB = new HashMap<>();
    }

    private static Comparator<Integer> getComparator(PriorityStrategy priorityStrategy, ABDir dir) {
        return Comparator.comparingDouble(i -> priorityStrategy.apply(i, dir));
    }

    private static Map<AlgorithmMode, AlgorithmFactory> factoryMap = new HashMap<>();

    static {
        factoryMap.put(DIJKSTRA, new DijkstraFactory());
        factoryMap.put(BI_DIJKSTRA, new BiDijkstraFactory());
        factoryMap.put(A_STAR, new AStarFactory());
        factoryMap.put(BI_A_STAR_CONSISTENT, new BiAStarMakeConsistentFactory());
        factoryMap.put(BI_A_STAR_SYMMETRIC, new BiAStarSymmetricFactory());
        factoryMap.put(A_STAR_LANDMARKS, new LandmarksFactory());
        factoryMap.put(BI_A_STAR_LANDMARKS, new BiLandmarksFactory());
    }

    public static void applyFactory(AlgorithmFactory factory) {
        factory.getPreprocessStrategy().process();
        biDirectional = factory.isBiDirectional();
        heuristicFunction = factory.getHeuristicFunction();
        terminationStrategy = factory.getTerminationStrategy();
        relaxStrategyA = factory.getRelaxStrategy();
        relaxStrategyB = factory.getRelaxStrategy();
        priorityStrategyA = factory.getPriorityStrategy();
        priorityStrategyB = factory.getPriorityStrategy();
        priorityQueueGetter = factory.getPriorityQueue();
    }

    // Path finding

    public static ShortestPathResult findShortestPath(int sourceP, int targetP, AlgorithmMode modeP) {
        if (sourceP == targetP) {
            return new ShortestPathResult(0, singletonList(sourceP), 0);
        }
        applyFactory(factoryMap.get(modeP));
        initFields(modeP, sourceP, targetP);
        initDataStructures();
        return biDirectional ? biDirectional() : oneDirectional();
    }

    private static ShortestPathResult oneDirectional() {
        List<List<Edge>> adjList = graph.getAdjList();
        estimatedDistA.put(source, 0.0);
        queueA.insert(source);

        while (!queueA.isEmpty()) {
            if (queueA.peek() == target || pathMapA.size() > adjList.size()) break;
            takeStep(adjList, A, false);
        }

        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, target);
        return new ShortestPathResult(nodeDistA.get(target), shortestPath, visitedA.size());
    }

    private static void takeStep(List<List<Edge>> adjList, ABDir dir, boolean biDirectional) {
        ABDir revDir = dir == A ? B : A;
        Integer currentNode = getQueue(dir).poll();
        if (currentNode == null) {
            return;
        }

        getVisited(dir).add(currentNode);
        for (Edge edge : adjList.get(currentNode)) {
            if (!getVisited(revDir).contains(edge.to)) {
                getRelaxStrategy(dir).relax(currentNode, edge, dir);
                if (biDirectional && getNodeDist(dir).get(currentNode) + edge.d + getNodeDist(revDir).get(edge.to) < goalDistance) {
                    goalDistance = getNodeDist(dir).get(currentNode) + edge.d + getNodeDist(revDir).get(edge.to);
                    middlePoint = edge.to;
                }
            }
        }
    }

    private static ShortestPathResult biDirectional() {
        // Implementation pseudocode from https://www.cs.princeton.edu/courses/archive/spr06/cos423/Handouts/EPP%20shortest%20path%20algorithms.pdf
        List<List<Edge>> adjList = graph.getAdjList();
        List<List<Edge>> revAdjList = graph.reverseAdjacencyList(adjList);

        // A-direction
        estimatedDistA.put(source, 0.0);
        queueA.insert(source);

        // B-direction
        estimatedDistB.put(target, 0.0);
        queueB.insert(target);

        goalDistance = Double.MAX_VALUE;
        middlePoint = -1;
        // Both queues need to be empty and an intersection has to be found in order to exit the while loop.
        while (!terminationStrategy.checkTermination(goalDistance) && (!queueA.isEmpty() && !queueB.isEmpty())) {
            if (queueA.size() + visitedA.size() < queueB.size() + visitedB.size()) {
                takeStep(adjList, A, true);
            } else {
                takeStep(revAdjList, B, true);
            }
        }

        if (middlePoint == -1) {
            return new ShortestPathResult(Double.MAX_VALUE, new LinkedList<>(), 0);
        }
        List<Integer> shortestPath = extractPathBi(adjList, revAdjList);
        double distance = goalDistance;
        return new ShortestPathResult(distance, shortestPath, visitedA.size() + visitedB.size());
    }

    public static ShortestPathResult singleToAllPath(int sourceP) {
        applyFactory(new DijkstraFactory());
        initFields(DIJKSTRA, sourceP, 0);
        initDataStructures();
        List<List<Edge>> adjList = graph.getAdjList();
        queueA.insert(source);

        while (!queueA.isEmpty()) {
            takeStep(adjList, A, false);
        }
        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, target);
        return new ShortestPathResult(0, shortestPath, visitedA.size(), nodeDistA, pathMapA);
    }

    private static List<Integer> extractPathBi(List<List<Edge>> adjList, List<List<Edge>> revAdjList) {
        List<Integer> shortestPathA = extractPath(pathMapA, adjList, source, middlePoint);
        List<Integer> shortestPathB = extractPath(pathMapB, revAdjList, target, middlePoint);
        graph.reversePaintEdges(revAdjList, adjList);
        shortestPathB.remove(shortestPathB.size() - 1);
        Collections.reverse(shortestPathB);
        shortestPathA.addAll(shortestPathB);
        return shortestPathA;
    }

    private static List<Integer> extractPath(Map<Integer, Integer> pathMap, List<List<Edge>> adjList, int from, int to) {
        Integer curNode = to;
        int prevNode = to;
        List<Integer> path = new ArrayList<>(pathMap.size());
        path.add(to);
        while (curNode != from) {
            curNode = pathMap.get(curNode);
            if (curNode == null) {
                return new ArrayList<>(0);
            }

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

    public static ShortestPathResult randomPath(AlgorithmMode modeP) {
        int n = graph.getNodeAmount();
        Random random = new Random(seed);
        int sourceR = random.nextInt(n);
        int targetR = random.nextInt(n);
        ShortestPathResult res = findShortestPath(sourceR, targetR, modeP);
        if (traceResult) {
            System.out.println("Distance from " + source + " to " + target + " is " + res.d);
            System.out.println("Graph has " + n + " nodes.");
        }
        return res;
    }

    /*public static void trace(MinPriorityQueue nodeQueue, ABDir dir) {
        MinPriorityQueue<Integer> copy = new PriorityQueue<Integer>(nodeQueue);
        if (trace && mode == BI_A_STAR_LANDMARKS) {
            System.out.print("NodeQueue: ");
            while (copy.size() != 0) {
                Integer object = copy.poll();
                System.out.print("(" + object + ": " + getPriorityFunction().apply(object, dir) + ")");
            }
            System.out.println();
        }
    }*/

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

    private static void traceRelax(Integer currentNode, Edge edge) {
        if (trace) {
            System.out.println("From " + currentNode + " to " + edge.to + " d = " + edge.d);
        }
    }

    public static void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        SSSP.distanceStrategy = distanceStrategy;
    }

    public static BiFunction<Node, Node, Double> getDistanceStrategy() {
        return distanceStrategy;
    }

    public static HeuristicFunction getHeuristicFunction() {
        return heuristicFunction;
    }

    public static PriorityStrategy getPriorityFunction() {
        return priorityStrategyA;
    }

    public static int getSource() {
        return source;
    }

    public static int getTarget() {
        return target;
    }

    public static Graph getGraph() {
        return graph;
    }

    public static void setGraph(Graph graph) {
        SSSP.graph = graph;
    }

    public static double[][] getLandmarkArray() {
        return landmarkArray;
    }

    public static void setLandmarkArray(double[][] pLandmarkArray) {
        landmarkArray = pLandmarkArray;
    }

    public static RelaxStrategy getRelaxStrategy(ABDir dir) {
        return dir == A ? relaxStrategyA : relaxStrategyB;
    }

    public static Set<Integer> getVisited(ABDir dir) {
        return dir == A ? visitedA : visitedB;
    }

    public static List<Double> getNodeDist(ABDir dir) {
        return dir == A ? nodeDistA : nodeDistB;
    }

    public static Map<Integer, Integer> getPathMap(ABDir dir) {
        return dir == A ? pathMapA : pathMapB;
    }

    public static MinPriorityQueue getQueue(ABDir dir) {
        return dir == A ? queueA : queueB;
    }

    public static Map<Integer, Double> getEstimatedDist(ABDir dir) {
        return dir == A ? estimatedDistA : estimatedDistB;
    }
}
