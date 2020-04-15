package paths;

import datastructures.MinPriorityQueue;
import model.*;
import paths.factory.*;
import paths.strategy.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static java.util.Collections.singletonList;
import static paths.AlgorithmMode.*;
import static paths.ABDir.*;
import static paths.Util.revDir;

public class SSSP {

    public static boolean trace = false;
    public static boolean traceResult = false;
    public static int seed = 0;

    private static Graph graph;
    private static Landmarks landmarks;
    private static int source, target;
    private static AlgorithmMode mode;
    private static double goalDistance;
    private static int middlePoint;
    private static double[][] landmarkArray;
    private static double[] reachBounds;

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
    private static MinPriorityQueue queueA;                 // Queue to hold the paths from Node: source
    private static MinPriorityQueue queueB;                 // Queue to hold the paths from Node: target
    private static double[] heuristicValuesA;
    private static double[] heuristicValuesB;
    private static GetPQueueStrategy priorityQueueGetter;
    private static Set<Integer> prunedSet;

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
        heuristicValuesA = initHeuristicValues(graph.getNodeAmount());
        heuristicValuesB = initHeuristicValues(graph.getNodeAmount());
        prunedSet = new LinkedHashSet<>();
    }

    private static double[] initHeuristicValues(int nodeAmount) {
        double[] arr = new double[nodeAmount];
        Arrays.fill(arr, -1.0);
        return arr;
    }

    public static void updatePriority(int nodeToUpdate, ABDir dir) {
        if (dir == A) heuristicValuesA[nodeToUpdate] = priorityStrategyA.apply(nodeToUpdate, dir);
        else heuristicValuesB[nodeToUpdate] = priorityStrategyB.apply(nodeToUpdate, dir);
        getQueue(dir).updatePriority(nodeToUpdate);
    }

    private static Comparator<Integer> getComparator(PriorityStrategy priorityStrategy, ABDir dir) {
        return (i, j) -> {
            if (dir == A) {
                double diff = Math.abs(heuristicValuesA[i] - heuristicValuesA[j]);
                if (diff <= 0.000000000000001) {
                    return i.compareTo(j);
                } else {
                    return Double.compare(heuristicValuesA[i], heuristicValuesA[j]);
                }
            } else {
                double diff = Math.abs(heuristicValuesB[i] - heuristicValuesB[j]);
                if (diff <= 0.000000000000001) {
                    return i.compareTo(j);
                } else {
                    return Double.compare(heuristicValuesB[i], heuristicValuesB[j]);
                }
            }

        };
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
        factoryMap.put(REACH, new ReachFactory());
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
            return new ShortestPathResult(0, singletonList(sourceP), 0, 0);
        }
        applyFactory(factoryMap.get(modeP));
        initFields(modeP, sourceP, targetP);
        initDataStructures();
        return biDirectional ? biDirectional() : oneDirectional();
    }

    private static ShortestPathResult oneDirectional() {
        long startTime = System.nanoTime();
        List<List<Edge>> adjList = graph.getAdjList();
        queueA.insert(source);

        while (!queueA.isEmpty()) {
            if (queueA.peek() == target || pathMapA.size() > adjList.size()) break;
            takeStep(adjList, A);
        }
        long endTime = System.nanoTime();
        long duration = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        Set<Integer> a = getPrunedSet();
        /*if (mode == REACH && source == 5087) {
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(
                        new OutputStreamWriter(new FileOutputStream("test.txt"), "UTF-8"));
                for (double s : getReachBounds()) {
                    pw.println(s);
                }
                pw.flush();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                e.printStackTrace();
            } finally {
                pw.close();
            }
        }
*/
        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, target);
        return new ShortestPathResult(nodeDistA.get(target), shortestPath, visitedA.size(), duration);
    }

    private static void takeStep(List<List<Edge>> adjList, ABDir dir) {
        Integer currentNode = getQueue(dir).poll();
        if (currentNode == null) {
            return;
        }

        getVisited(dir).add(currentNode);
        for (Edge edge : adjList.get(currentNode)) {
            assert !getVisited(revDir(dir)).contains(edge.to); // By no scan overlap-theorem
            getRelaxStrategy(dir).relax(currentNode, edge, dir);
        }
    }

    private static ShortestPathResult biDirectional() {
        long startTime = System.nanoTime();
        // Implementation pseudocode from https://www.cs.princeton.edu/courses/archive/spr06/cos423/Handouts/EPP%20shortest%20path%20algorithms.pdf
        List<List<Edge>> adjList = graph.getAdjList();
        List<List<Edge>> revAdjList = graph.getReverse(adjList);

        // A-direction
        queueA.insert(source);
        // B-direction
        queueB.insert(target);

        goalDistance = Double.MAX_VALUE;
        middlePoint = -1;
        // Both queues need to be empty and an intersection has to be found in order to exit the while loop.
        while (!terminationStrategy.checkTermination(goalDistance) && (!queueA.isEmpty() && !queueB.isEmpty())) {
            if (queueA.size() + visitedA.size() < queueB.size() + visitedB.size()) {
                takeStep(adjList, A);
            } else {
                takeStep(revAdjList, B);
            }
        }
        long endTime = System.nanoTime();
        long duration = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);

        if (middlePoint == -1) {
            return new ShortestPathResult(Double.MAX_VALUE, new LinkedList<>(), 0, 0);
        }
        List<Integer> shortestPath = extractPathBi(adjList, revAdjList);
        double distance = goalDistance;
        return new ShortestPathResult(distance, shortestPath, visitedA.size() + visitedB.size(), duration);
    }

    public static ShortestPathResult singleToAllPath(int sourceP) {
        applyFactory(new DijkstraFactory());
        initFields(DIJKSTRA, sourceP, 0);
        initDataStructures();
        long startTime = System.nanoTime();
        List<List<Edge>> adjList = graph.getAdjList();
        queueA.insert(source);

        while (!queueA.isEmpty()) {
            takeStep(adjList, A);
        }
        long endTime = System.nanoTime();
        long duration = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, target);
        return new ShortestPathResult(0, shortestPath, visitedA.size(), nodeDistA, pathMapA, duration);
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

    private static void traceRelax(Integer currentNode, Edge edge) {
        if (trace) {
            System.out.println("From " + currentNode + " to " + edge.to + " d = " + edge.d);
        }
    }

    // Public Access Methods
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

   /* public static Map<Integer, Double> getEstimatedDist(ABDir dir) {
        return dir == A ? estimatedDistA : estimatedDistB;
    }*/

    public static Landmarks getLandmarks() {
        return landmarks;
    }

    public static void setLandmarks(Landmarks landmarks) {
        SSSP.landmarks = landmarks;
    }

    public static double[] getReachBounds() {
        return reachBounds;
    }

    public static void setReachBounds(double[] bounds) {
        reachBounds = bounds;
    }

    public static Set<Integer> getPrunedSet() {
        return prunedSet;
    }

    public static double getGoalDistance() {
        return goalDistance;
    }

    public static void setGoalDistance(double goalDistance) {
        SSSP.goalDistance = goalDistance;
    }

    public static void setMiddlePoint(int middlePoint) {
        SSSP.middlePoint = middlePoint;
    }
}
