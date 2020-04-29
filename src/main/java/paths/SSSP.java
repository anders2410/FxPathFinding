package paths;

import datastructures.MinPriorityQueue;
import javafx.util.Pair;
import model.*;
import paths.factory.*;
import paths.strategy.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static paths.AlgorithmMode.*;
import static paths.ABDir.*;
import static paths.Util.revDir;
import static paths.generator.GetPQueueGenerator.getJavaQueue;

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
    private static List<Double> reachBounds;

    private static boolean biDirectional;
    private static BiFunction<Node, Node, Double> distanceStrategy;
    private static HeuristicFunction heuristicFunction;
    private static TerminationStrategy terminationStrategy;
    private static AlternationStrategy alternationStrategy;

    private static RelaxStrategy relaxStrategyA;
    private static RelaxStrategy relaxStrategyB;

    private static PriorityStrategy priorityStrategyA;
    private static PriorityStrategy priorityStrategyB;

    private static List<Double> nodeDistA;
    private static List<Double> nodeDistB;
    private static Set<Integer> scannedA;                   // A set of visited nodes starting from Node: source
    private static Set<Integer> scannedB;                   // A set of visited nodes starting from Node: target
    private static Set<Edge> relaxedA;
    private static Set<Edge> relaxedB;
    private static Map<Integer, Integer> pathMapA;
    private static Map<Integer, Integer> pathMapB;
    private static MinPriorityQueue queueA;                 // Queue to hold the paths from Node: source
    private static MinPriorityQueue queueB;                 // Queue to hold the paths from Node: target
    private static double[] heuristicValuesA;
    private static double[] heuristicValuesB;
    private static GetPQueueStrategy priorityQueueGetter;
    private static Set<Integer> prunedSet;
    private static double singleToAllBound;
    private static ContractionHierarchiesResult contractionHierarchiesResult;

    // Initialization
    private static void initFields(AlgorithmMode modeP, int sourceP, int targetP) {
        mode = modeP;
        source = sourceP;
        target = targetP;
    }

    private static void initDataStructures() {
        nodeDistA = initNodeDist(source, graph.getNodeAmount());
        nodeDistB = initNodeDist(target, graph.getNodeAmount());
        scannedA = new HashSet<>();
        scannedB = new HashSet<>();
        relaxedA = new HashSet<>();
        relaxedB = new HashSet<>();
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
        factoryMap.put(BI_DIJKSTRA_SAME_DIST, new BiDijkstraSameDistFactory());
        factoryMap.put(A_STAR, new AStarFactory());
        factoryMap.put(BI_A_STAR_CONSISTENT, new BiAStarMakeConsistentFactory());
        factoryMap.put(BI_A_STAR_SYMMETRIC, new BiAStarSymmetricFactory());
        factoryMap.put(A_STAR_LANDMARKS, new LandmarksFactory());
        factoryMap.put(BI_A_STAR_LANDMARKS, new BiLandmarksFactory());
        factoryMap.put(REACH, new ReachFactory());
        factoryMap.put(BI_REACH, new BiReachFactory());
        factoryMap.put(REACH_A_STAR, new ReachAStarFactory());
        factoryMap.put(BI_REACH_A_STAR, new BiReachAStarFactory());
        factoryMap.put(REACH_LANDMARKS, new ReachLandmarksFactory());
        factoryMap.put(CONTRACTION_HIERARCHIES, new ContractionHierarchiesFactory());
        factoryMap.put(SINGLE_TO_ALL, new OneToAllDijkstra());
        factoryMap.put(BOUNDED_SINGLE_TO_ALL, new BoundedOneToAll());
    }

    public static void applyFactory(AlgorithmFactory factory) {
        factory.getPreProcessStrategy().process();
        biDirectional = factory.isBiDirectional();
        heuristicFunction = factory.getHeuristicFunction();
        terminationStrategy = factory.getTerminationStrategy();
        relaxStrategyA = factory.getRelaxStrategy();
        relaxStrategyB = factory.getRelaxStrategy();
        priorityStrategyA = factory.getPriorityStrategy();
        priorityStrategyB = factory.getPriorityStrategy();
        priorityQueueGetter = getJavaQueue();
        alternationStrategy = factory.getAlternationStrategy();
    }

    // Path finding
    public static ShortestPathResult findShortestPath(int sourceP, int targetP, AlgorithmMode modeP) {
        if (sourceP == targetP && modeP != BOUNDED_SINGLE_TO_ALL) {
            return new ShortestPathResult();
        }
        applyFactory(factoryMap.get(modeP));
        initFields(modeP, sourceP, targetP);
        initDataStructures();
        return biDirectional ? biDirectional() : oneDirectional(); //TODO: Make one directional ALT work in bidirectional
    }

    private static ShortestPathResult oneDirectional() {
        long startTime = System.nanoTime();
        List<List<Edge>> adjList = graph.getAdjList();
        queueA.insert(source);

        while (!queueA.isEmpty()) {
            /*if (queueA.peek() == target || pathMapA.size() > adjList.size()) break;*/
            if (queueA.peek() == target && (mode != BOUNDED_SINGLE_TO_ALL && mode != SINGLE_TO_ALL)) break;
            takeStep(adjList, A);
        }
        long endTime = System.nanoTime();
        long duration = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        /*Set<Integer> a = getPrunedSet();
        if (mode == REACH && source == 5087) {
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
        List<Integer> shortestPath = extractPath(pathMapA, source, target);
        // TODO: 25-04-2020 Strategy pattern this
        if (mode == SINGLE_TO_ALL || mode == BOUNDED_SINGLE_TO_ALL)
            return new ShortestPathResult(0, shortestPath, scannedA, relaxedA, nodeDistA, pathMapA, duration);
        return new ShortestPathResult(nodeDistA.get(target), shortestPath, scannedA, relaxedA, duration);
    }

    private static void takeStep(List<List<Edge>> adjList, ABDir dir) {
        Integer currentNode = getQueue(dir).poll();
        if (currentNode == null) {
            return;
        }

        getScanned(dir).add(currentNode);
        for (Edge edge : adjList.get(currentNode)) {
            //assert !getVisited(revDir(dir)).contains(edge.to) || currentNode == target || currentNode == source; // By no scan overlap-theorem
            if (!getScanned(revDir(dir)).contains(edge.to)) {
                getRelaxStrategy(dir).relax(edge, dir);
            }
        }
    }

    // Implementation pseudo code from https://www.cs.princeton.edu/courses/archive/spr06/cos423/Handouts/EPP%20shortest%20path%20algorithms.pdf
    private static ShortestPathResult biDirectional() {
        long startTime = System.nanoTime();

        List<List<Edge>> adjList = graph.getAdjList();
        List<List<Edge>> revAdjList = graph.getReverse(adjList);
        queueA.insert(source);
        queueB.insert(target);

        goalDistance = Double.MAX_VALUE;
        middlePoint = -1;
        // Both queues need to be empty or an intersection has to be found in order to exit the while loop.
        while (!terminationStrategy.checkTermination(goalDistance) && (!queueA.isEmpty() && !queueB.isEmpty())) {
            if (alternationStrategy.check()) {
                takeStep(adjList, A);
            } else {
                takeStep(revAdjList, B);
            }
        }
        long endTime = System.nanoTime();
        long duration = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);

        if (middlePoint == -1) {
            return new ShortestPathResult();
        }

        // TODO: 28/04/2020 Do something about this. Maybe strategy pattern?
        if (mode == CONTRACTION_HIERARCHIES) {
            // Goes through all overlapping nodes and find the one with the smallest distance.
            double finalDistance = Double.MAX_VALUE;
            for (int node : scannedA) {
                if (scannedA.contains(node) && scannedB.contains(node)) {
                    // Replace if lower than actual
                    double distance = nodeDistA.get(node) + nodeDistB.get(node);
                    if (0 <= distance && distance < finalDistance) {
                        finalDistance = distance;
                        setMiddlePoint(node);
                    }
                }
            }

            System.out.println("Another MiddlePoint: " + middlePoint);
            List<Integer> shortestPathCH = extractPathBi();

            Set<Integer> result = new LinkedHashSet<>();
            for (int i = 0; i < shortestPathCH.size() - 1; i++) {
                List<Integer> contractedNodes = contractionHierarchiesResult.getShortcuts().get(new Pair<>(shortestPathCH.get(i), shortestPathCH.get(i + 1)));
                result.add(shortestPathCH.get(i));
                if (contractedNodes != null) {
                    result.addAll(contractedNodes);
                }
                result.add(shortestPathCH.get(i + 1));
            }

            return new ShortestPathResult(goalDistance, new ArrayList<>(result), scannedA, scannedB, relaxedA, relaxedB, duration);
        }

        List<Integer> shortestPath = extractPathBi();
        return new ShortestPathResult(goalDistance, shortestPath, scannedA, scannedB, relaxedA, relaxedB, duration);
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
        List<Integer> shortestPath = extractPath(pathMapA, source, target);
        return new ShortestPathResult(0, shortestPath, scannedA, relaxedA, nodeDistA, pathMapA, duration);
    }

    private static List<Integer> extractPathBi() {
        List<Integer> shortestPathA = extractPath(pathMapA, source, middlePoint);
        List<Integer> shortestPathB = extractPath(pathMapB, target, middlePoint);
        shortestPathB.remove(shortestPathB.size() - 1);
        Collections.reverse(shortestPathB);
        shortestPathA.addAll(shortestPathB);
        return shortestPathA;
    }

    private static List<Integer> extractPath(Map<Integer, Integer> pathMap, int from, int to) {
        Integer curNode = to;
        List<Integer> path = new ArrayList<>(pathMap.size());
        path.add(to);
        while (curNode != from) {
            curNode = pathMap.get(curNode);
            if (curNode == null) {
                return new ArrayList<>(0);
            }

            path.add(curNode);
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

    public static PriorityStrategy getPriorityStrategy() {
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

    public static Set<Integer> getScanned(ABDir dir) {
        return dir == A ? scannedA : scannedB;
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

    public static List<Double> getReachBounds() {
        return reachBounds;
    }

    public static void setReachBounds(List<Double> bounds) {
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

    public static double getSingleToAllBound() {
        return singleToAllBound;
    }

    public static void setSingleToAllBound(double singleToAllBound) {
        SSSP.singleToAllBound = singleToAllBound;
    }

    public static void putRelaxedEdge(ABDir dir, Edge edge) {
        (dir == A ? relaxedA : relaxedB).add(edge);
    }

    public static ContractionHierarchiesResult getContractionHierarchiesResult() {
        return contractionHierarchiesResult;
    }

    public static void setContractionHierarchiesResult(ContractionHierarchiesResult contractionHierarchiesResult) {
        SSSP.contractionHierarchiesResult = contractionHierarchiesResult;
    }
}
