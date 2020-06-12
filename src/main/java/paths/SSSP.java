package paths;

import datastructures.MinPriorityQueue;
import info_model.GraphInfo;
import model.Edge;
import model.Graph;
import model.Node;
import paths.factory.*;
import paths.factory.DuplicateFactories.*;
import paths.generator.EdgeWeightGenerator;
import paths.generator.RelaxGenerator;
import paths.preprocessing.CHResult;
import paths.preprocessing.Landmarks;
import paths.strategy.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static paths.ABDir.A;
import static paths.ABDir.B;
import static paths.AlgorithmMode.*;
import static paths.Util.revDir;

public class SSSP {
    public static boolean trace = false;
    public static boolean traceResult = false;
    public static int seed = 0;

    private static Graph graph;
    private static List<List<Edge>> originalRevAdjList;
    private static Graph CHGraph;
    private static List<List<Edge>> CHRevAdjList;
    private static GraphInfo graphInfo;
    private static Landmarks landmarks;
    private static int source, target;
    private static AlgorithmMode mode;
    private static double goalDistance;
    private static int middlePoint;
    private static double[][] landmarkArray;
    private static List<Double> reachBounds;
    private static List<Integer> densityMeasures;
    private static List<Double> densityMeasuresNorm;
    private static List<Boolean> stalled;

    // All the different strategies!
    private static boolean biDirectional;
    private static BiFunction<Node, Node, Double> distanceStrategy;
    private static EdgeWeightStrategy edgeWeightStrategy = EdgeWeightGenerator.getDistanceWeights();
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
    private static double singleToAllBound;
    private static CHResult chResult;
    private static double bestPathLengthSoFar;
    private static ScanPruningStrategy scanPruningStrategy;
    private static ResultPackingStrategy resultPackingStrategy;
    private static List<List<Edge>> adjList;
    private static List<List<Edge>> revAdjList;
    private static QueueUpdatingStrategy updatePriorityQueueStrategy;
    private static QueuePollingStrategy pollPriorityQueueStrategy;

    // Initialization
    private static void initFields(AlgorithmMode modeP, int sourceP, int targetP) {
        mode = modeP;
        if (allowFlip && densityMeasuresNorm != null && densityMeasuresNorm.get(sourceP) > densityMeasuresNorm.get(targetP)) {
            SSSP.target = sourceP;
            SSSP.source = targetP;
        } else {
            SSSP.source = sourceP;
            SSSP.target = targetP;
            allowFlip = false;
        }
    }

    private static void initDataStructures() {
        // We should move these into the same for-loop..
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
        bestPathLengthSoFar = Double.MAX_VALUE;

        /*stalled = new ArrayList<>(graph.getNodeAmount());
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            stalled.add(false);
        }*/

        if (allowFlip) {
            List<List<Edge>> adjList = getAdjList();
            List<List<Edge>> revAdjList = getRevAdjList();
            setAdjList(revAdjList);
            setRevAdjList(adjList);
        } else if (mode == CONTRACTION_HIERARCHIES || mode == DUPLICATE_CONTRACTION_HIERARCHIES) {
            adjList = CHGraph.getAdjList();
            revAdjList = CHRevAdjList;
        } else {
            adjList = graph.getAdjList();
            revAdjList = originalRevAdjList;
        }
    }

    private static double[] initHeuristicValues(int nodeAmount) {
        double[] arr = new double[nodeAmount];
        Arrays.fill(arr, -1.0);
        return arr;
    }

    public static double[] getHeuristicValuesA() {
        return heuristicValuesA;
    }

    public static double[] getHeuristicValuesB() {
        return heuristicValuesB;
    }

    public static PriorityStrategy getPriorityStrategyA() {
        return priorityStrategyA;
    }

    public static PriorityStrategy getPriorityStrategyB() {
        return priorityStrategyB;
    }

    public static void updatePriority(int nodeToUpdate, ABDir dir) {
        updatePriorityQueueStrategy.updatePriority(nodeToUpdate, dir);
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
        factoryMap.put(BI_DIJKSTRA_DENSITY, new BiDijkstraDensityFactory());
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
        factoryMap.put(BI_REACH_LANDMARKS, new BiReachLandmarksFactory());
        factoryMap.put(CONTRACTION_HIERARCHIES, new ContractionHierarchiesFactory());
        factoryMap.put(SINGLE_TO_ALL, new OneToAllDijkstra());
        factoryMap.put(BOUNDED_SINGLE_TO_ALL, new BoundedOneToAll());
        factoryMap.put(CONTRACTION_HIERARCHIES_LANDMARKS, new ContractionHierarchiesLandmarksFactory());
        factoryMap.put(DUPLICATE_DIJKSTRA, new DijkstraDuplicateQueueFactory());
        factoryMap.put(DUPLICATE_A_STAR, new AStarDuplicate());
        factoryMap.put(DUPLICATE_A_STAR_LANDMARKS, new LandmarksDuplicateFactory());
        factoryMap.put(DUPLICATE_BI_A_STAR_CONSISTENT, new BiAstarConsistenDuplicateFactory());
        factoryMap.put(DUPLICATE_BI_A_STAR_LANDMARKS, new BiLandmarksDuplicateFactory());
        factoryMap.put(DUPLICATE_BI_DIJKSTRA, new BiDijkstraDuplicateFactory());
        factoryMap.put(DUPLICATE_BI_REACH, new BiReachDuplicateFactory());
        factoryMap.put(DUPLICATE_BI_REACH_A_STAR, new BiReachAstarDuplicateFactory());
        factoryMap.put(DUPLICATE_BI_REACH_LANDMARKS, new BiReachLandmarksDuplicateFactory());
        factoryMap.put(DUPLICATE_REACH, new ReachDuplicateFactory());
        factoryMap.put(DUPLICATE_REACH_A_STAR, new ReachAstarDuplicateFactory());
        factoryMap.put(DUPLICATE_REACH_LANDMARKS, new ReachLandmarksDuplicateFactory());
        factoryMap.put(DUPLICATE_CONTRACTION_HIERARCHIES, new ContractionHierarchiesDuplicateFactory());
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
        priorityQueueGetter = factory.getQueue();
        alternationStrategy = factory.getAlternationStrategy();
        scanPruningStrategy = factory.getScanPruningStrategy();
        resultPackingStrategy = factory.getResultPackingStrategy();
        updatePriorityQueueStrategy = factory.getQueueUpdatingStrategy();
    }

    // Path finding
    public static ShortestPathResult findShortestPath(int sourceP, int targetP, AlgorithmMode modeP) {
        if (sourceP == targetP && modeP != BOUNDED_SINGLE_TO_ALL && modeP != SINGLE_TO_ALL) {
            return new ShortestPathResult();
        }
        applyFactory(factoryMap.get(modeP));
        initFields(modeP, sourceP, targetP);
        initDataStructures();
        // TODO: Make one directional ALT work in bidirectional
        return biDirectional ? biDirectional() : oneDirectional();
    }

    public static boolean allowFlip = false;

    private static ShortestPathResult oneDirectional() {
        queueA.insert(source);
        long startTime = System.nanoTime();

        while (!queueA.isEmpty() && !terminationStrategy.checkTermination(getGoalDistance())) {
            /*if (queueA.peek() == target || pathMapA.size() > adjList.size()) break;*/
            if (queueA.nodePeek() == target && (mode != BOUNDED_SINGLE_TO_ALL && mode != SINGLE_TO_ALL)) break;
            takeStep(adjList, A);
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        return resultPackingStrategy.packResult(duration);
    }

    private static ShortestPathResult biDirectional() {
        queueA.insert(source);
        queueB.insert(target);

        goalDistance = Double.MAX_VALUE;
        middlePoint = -1;

        long startTime = System.nanoTime();
        // Both queues need to be empty or an intersection has to be found in order to exit the while loop.
        while (!terminationStrategy.checkTermination(goalDistance) && (!queueA.isEmpty() || !queueB.isEmpty())) {
            if (alternationStrategy.check()) {
                takeStep(adjList, A);
            } else {
                takeStep(revAdjList, B);
            }
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        return resultPackingStrategy.packResult(duration);
    }

    private static void takeStep(List<List<Edge>> adjList, ABDir dir) {
        Integer currentNode = extractMinNode(dir);
        if (scanPruningStrategy.checkPrune(dir, currentNode)) return;
        getScanned(dir).add(currentNode);
        // stalled.set(currentNode, true);
        // if (stalled.get(currentNode)) return;
        for (Edge edge : adjList.get(currentNode)) {
            getRelaxStrategy(dir).relax(edge, dir);
            // Stall-on-demand heuristic. Helps prune the search space!
            /*if (mode == CONTRACTION_HIERARCHIES) {
                if (getNodeDist(revDir(dir)).get(edge.to) + edge.d < getNodeDist(revDir(dir)).get(edge.from)) {
                    SSSP.getStalled().set(edge.from, true);
                    // System.out.println(getStalled().get(edge.from));
                    break;
                }
            }*/
        }
    }

    private static Integer extractMinNode(ABDir dir) {
        return getQueue(dir).nodePoll();
    }

    public static boolean reverseMe = false;

    public static ShortestPathResult singleToAllPath(int sourceP) {
        applyFactory(factoryMap.get(SINGLE_TO_ALL));
        initFields(SINGLE_TO_ALL, sourceP, 0);
        initDataStructures();
        long startTime = System.nanoTime();
        List<List<Edge>> adjList = graph.getAdjList();
        queueA.insert(source);
        while (!queueA.isEmpty()) {
            takeStep(adjList, A);
        }
        List<Integer> shortestPath = extractPath(pathMapA, source, target);
        long endTime = System.nanoTime();
        long duration = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        return new ShortestPathResult(0, shortestPath, scannedA, relaxedA, nodeDistA, pathMapA, duration);
    }

    public static List<Integer> extractPathBi() {
        List<Integer> shortestPathA = extractPath(pathMapA, source, middlePoint);
        List<Integer> shortestPathB = extractPath(pathMapB, target, middlePoint);
        if (!shortestPathB.isEmpty()) {
            shortestPathB.remove(shortestPathB.size() - 1);
        }
        Collections.reverse(shortestPathB);
        shortestPathA.addAll(shortestPathB);
        return shortestPathA;
    }

    public static List<Integer> extractPath(Map<Integer, Integer> pathMap, int from, int to) {
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
        List<Double> nodeDist = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            nodeDist.add(Double.MAX_VALUE);
        }
        nodeDist.set(from, 0.0);
        return nodeDist;
    }

    private static void traceRelax(Integer currentNode, Edge edge) {
        if (trace) {
            System.out.println("From " + currentNode + " to " + edge.to + " d = " + edgeWeightStrategy.getWeight(edge, A));
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

    public static GraphInfo getGraphInfo() {
        return graphInfo;
    }

    public static void setGraph(Graph graph) {
        SSSP.graph = graph;
        SSSP.adjList = graph.getAdjList();
        SSSP.originalRevAdjList = graph.getReverse(adjList);
    }

    public static void setGraphInfo(GraphInfo graphInfo) {
        SSSP.graphInfo = graphInfo;
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

    public static void setAlternationStrategy(AlternationStrategy alternationStrategy) {
        SSSP.alternationStrategy = alternationStrategy;
    }

    public static Set<Edge> getRelaxed(ABDir dir) {
        return dir == A ? relaxedA : relaxedB;
    }

    public static Set<Integer> getScanned(ABDir dir) {
        return dir == A ? scannedA : scannedB;
    }

    public static void setScanned(ABDir dir, Set<Integer> newValue) {
        if (dir == A) {
            scannedA = newValue;
        } else {
            scannedB = newValue;
        }
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

    public static CHResult getCHResult() {
        return chResult;
    }

    public static void setCHResult(CHResult chResult) {
        SSSP.chResult = chResult;
        SSSP.CHGraph = chResult.getGraph();
        SSSP.CHRevAdjList = CHGraph.getReverse(CHGraph.getAdjList());
    }

    public static void setCHGraph(Graph CHGraph) {
        SSSP.CHGraph = CHGraph;
    }

    public static double getBestPathLengthSoFar() {
        return bestPathLengthSoFar;
    }

    public static void setBestPathLengthSoFar(double bestPathLengthSoFar) {
        SSSP.bestPathLengthSoFar = bestPathLengthSoFar;
    }

    public static int getMiddlePoint() {
        return middlePoint;
    }

    public static EdgeWeightStrategy getEdgeWeightStrategy() {
        return edgeWeightStrategy;
    }

    public static void setEdgeWeightStrategy(EdgeWeightStrategy edgeWeightStrategy) {
        RelaxGenerator.setEdgeWeightStrategy(edgeWeightStrategy);
        SSSP.edgeWeightStrategy = edgeWeightStrategy;
        setLandmarkArray(null);
    }

    public static void setDensityMeasures(List<Integer> densityMeasures) {
        SSSP.densityMeasures = densityMeasures;
        int maxDens = densityMeasures.stream().max(Integer::compareTo).orElse(1);
        densityMeasuresNorm = new ArrayList<>();
        for (int density : densityMeasures) {
            densityMeasuresNorm.add((double) density / maxDens);
        }
    }

    public static List<Integer> getDensityMeasures() {
        return densityMeasures;
    }

    public static List<Double> getDensityMeasuresNorm() {
        return densityMeasuresNorm;
    }

    public static List<List<Edge>> getAdjList() {
        return adjList;
    }

    public static void setAdjList(List<List<Edge>> adjList) {
        SSSP.adjList = adjList;
    }

    public static List<List<Edge>> getRevAdjList() {
        return revAdjList;
    }

    public static void setRevAdjList(List<List<Edge>> revAdjList) {
        SSSP.revAdjList = revAdjList;
    }

    public static List<Boolean> getStalled() {
        return stalled;
    }

    public static void setStalled(List<Boolean> stalled) {
        SSSP.stalled = stalled;
    }
}