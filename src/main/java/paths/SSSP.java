package paths;

import model.*;
import paths.factory.*;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Collections.singletonList;
import static paths.AlgorithmMode.*;
import static paths.ABDir.A;
import static paths.ABDir.B;

public class SSSP {

    public static boolean trace = false;
    public static boolean traceResult = false;
    public static int seed = 0;

    private static List<Node> nodeList;
    private static BiFunction<Node, Node, Double> distanceStrategy;

    private static HeuristicFunction heuristicFunction;
    private static TerminationStrategy terminationStrategy;
    private static PriorityStrategy priorityStrategy;
    private static RelaxStrategy relaxStrategy;
    private static Comparator<Integer> comparator;

    private static Graph graph;
    private static int source, target;
    private static AlgorithmMode mode;
    private static double goalDistance;
    private static int middlePoint;
    private static double[][] landmarkArray;

    private static AlgorithmFactory factory;

    private static List<Double> nodeDistA;
    private static List<Double> nodeDistB;
    private static Set<Integer> visitedA;
    private static Set<Integer> visitedB;
    private static Map<Integer, Integer> pathMapA;
    private static Map<Integer, Integer> pathMapB;
    private static PriorityQueue<Integer> queueA;
    private static PriorityQueue<Integer> queueB;
    private static Map<Integer, Double> estimatedDistA;
    private static Map<Integer, Double> estimatedDistB;

    private static void initializeGlobalFields(Graph graphP, AlgorithmMode modeP, int sourceP, int targetP) {
        mode = modeP;
        if (mode == AlgorithmMode.BI_A_STAR_LANDMARKS) {
            if (landmarkArray == null && !graphP.getLandmarks().isEmpty()) {
                landmarkArray = new double[32][graphP.getNodeAmount()];
                int index = 0;
                List<List<Edge>> originalList = graphP.getAdjList();
                for (Integer landmarkIndex : graphP.getLandmarks()) {
                    List<Double> forwardDistance = singleToAllPath(graphP, landmarkIndex).nodeDistance;
                    double[] arrForward = forwardDistance.stream().mapToDouble(Double::doubleValue).toArray();
                    graphP.setAdjList(graphP.reverseAdjacencyList(graphP.getAdjList()));
                    List<Double> backDistance = singleToAllPath(graphP, landmarkIndex).nodeDistance;
                    double[] arrBackward = backDistance.stream().mapToDouble(Double::doubleValue).toArray();
                    graphP.setAdjList(originalList);
                    landmarkArray[index] = arrForward;
                    landmarkArray[index + 1] = arrBackward;
                    index++;
                    index++;
                }
            }
        }
        mode = modeP;
        graph = graphP;
        nodeList = graph.getNodeList();
        source = sourceP;
        target = targetP;
    }

    public static ShortestPathResult singleToAllPath(Graph graphP, int sourceP) {
        graph = graphP;
        nodeList = graph.getNodeList();
        AlgorithmFactory dijkstraFactory = new DijkstraFactory();
        source = sourceP;
        List<List<Edge>> adjList = graph.getAdjList();
        nodeDistA = initNodeDist(source, adjList.size());
        PriorityStrategy priorityStrategy = dijkstraFactory.getPriorityStrategy();
        Comparator<Integer> comparator = getComparator(priorityStrategy, A);
        queueA = new PriorityQueue<>(comparator);
        queueA.add(source);
        Set<Integer> seenNodes = new LinkedHashSet<>();
        pathMapA = new HashMap<>();
        RelaxStrategy relaxStrategy = dijkstraFactory.getRelaxStrategy();

        while (!queueA.isEmpty()) {
            Integer currentNode = queueA.poll();
            if (seenNodes.contains(currentNode)) {
                continue;
            }
            seenNodes.add(currentNode);
            for (Edge edge : adjList.get(currentNode)) {
                relaxStrategy.relax(currentNode, edge, A);
                if (trace) {
                    System.out.println("From " + currentNode + " to " + edge.to + " d = " + edge.d);
                }
            }
        }
        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, target);
        return new ShortestPathResult(0, shortestPath, seenNodes.size(), nodeDistA);
    }

    private static Map<AlgorithmMode, AlgorithmFactory> factoryMap = new HashMap<>();
    static {
        factoryMap.put(DIJKSTRA, new DijkstraFactory());
        factoryMap.put(BI_DIJKSTRA, new BiDijkstraFactory());
        factoryMap.put(A_STAR, new AStarFactory());
        factoryMap.put(BI_A_STAR_CONSISTENT, new BiAStarConsistentFactory());
        factoryMap.put(BI_A_STAR_SYMMETRIC, new BiAStarSymmetricFactory());
        factoryMap.put(BI_A_STAR_LANDMARKS, new LandmarksFactory());
    }

    public static ShortestPathResult sssp(Graph graphP, int sourceP, int targetP, AlgorithmMode modeP) {
        factory = factoryMap.get(modeP);
        applyFactory(factory);
        initializeGlobalFields(graphP, modeP, sourceP, targetP);
        if (source == target) {
            return new ShortestPathResult(0, singletonList(source), 0);
        }
        ShortestPathResult result;
        if (mode == AlgorithmMode.BI_DIJKSTRA || mode == AlgorithmMode.BI_A_STAR_SYMMETRIC || mode == AlgorithmMode.BI_A_STAR_CONSISTENT || mode == AlgorithmMode.BI_A_STAR_LANDMARKS) {
            result = biDirectional();
        } else {
            result = oneDirectional();
        }
        return result;
    }

    private static void applyFactory(AlgorithmFactory factory) {
        heuristicFunction = factory.getHeuristicFunction();
        terminationStrategy = factory.getTerminationStrategy();
        priorityStrategy = factory.getPriorityStrategy();
        relaxStrategy = factory.getRelaxStrategy();
        comparator = getComparator(priorityStrategy, A);
    }

    private static Comparator<Integer> getComparator(PriorityStrategy priorityStrategy, ABDir dir) {
        return Comparator.comparingDouble((i) -> priorityStrategy.apply(i, dir));
    }

    private static ShortestPathResult oneDirectional() {
        List<List<Edge>> adjList = graph.getAdjList();
        nodeDistA = initNodeDist(source, adjList.size());
        if (mode == AlgorithmMode.A_STAR) {
            estimatedDistA = new HashMap<>();
            estimatedDistA.put(source, 0.0);
        }
        queueA = new PriorityQueue<>(comparator);
        queueA.add(source);
        Set<Integer> seenNodes = new LinkedHashSet<>();
        pathMapA = new HashMap<>();

        while (!queueA.isEmpty()) {
            Integer currentNode = queueA.poll();
            if (seenNodes.contains(currentNode)) {
                continue;
            }
            seenNodes.add(currentNode);
            if (currentNode == target) {
                break;
            }
            for (Edge edge : adjList.get(currentNode)) {
                relaxStrategy.relax(currentNode, edge, A);
                if (trace) {
                    System.out.println("From " + currentNode + " to " + edge.to + " d = " + edge.d);
                }
            }
            trace(queueA); //Print queue if trace
        }

        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, target);
        return new ShortestPathResult(nodeDistA.get(target), shortestPath, seenNodes.size());
    }

    private static ShortestPathResult biDirectional() {
        // Implementation pseudocode from https://www.cs.princeton.edu/courses/archive/spr06/cos423/Handouts/EPP%20shortest%20path%20algorithms.pdf
        // TODO: Try to integrate it with sssp Dijkstra implementation.
        // TODO: Implement Bi-ASTAR using consistent approach. Symmetric approach visits a lot of stuff.
        List<List<Edge>> adjList = graph.getAdjList();
        List<List<Edge>> revAdjList = graph.reverseAdjacencyList(adjList);

        // A-direction
        nodeDistA = initNodeDist(source, adjList.size());
        estimatedDistA = null;
        if (mode == AlgorithmMode.BI_A_STAR_SYMMETRIC || mode == AlgorithmMode.BI_A_STAR_CONSISTENT || mode == AlgorithmMode.BI_A_STAR_LANDMARKS) {
            estimatedDistA = new HashMap<>();
            estimatedDistA.put(source, 0.0);
        }

        // Queue to hold the paths from Node: source.
        queueA = new PriorityQueue<>(comparator);
        queueA.add(source);
        // A set of visited nodes starting from Node a.
        visitedA = new HashSet<>();
        pathMapA = new HashMap<>();

        // B-direction
        nodeDistB = initNodeDist(target, adjList.size());
        estimatedDistB = null;
        if (mode == AlgorithmMode.BI_A_STAR_SYMMETRIC || mode == AlgorithmMode.BI_A_STAR_CONSISTENT || mode == AlgorithmMode.BI_A_STAR_LANDMARKS) {
            estimatedDistB = new HashMap<>();
            estimatedDistB.put(target, 0.0);
        }
        PriorityStrategy priorityStrategyB = priorityStrategy;
        Comparator<Integer> comparatorB = getComparator(priorityStrategyB, B);

        // Queue to hold the paths from Node: to.
        queueB = new PriorityQueue<>(comparatorB);
        queueB.add(target);
        // A set of visited nodes starting from Node b.
        visitedB = new HashSet<>();
        pathMapB = new HashMap<>();
        RelaxStrategy relaxStrategyB = relaxStrategy;

        goalDistance = Double.MAX_VALUE;
        middlePoint = -1;
        // Both queues need to be empty and an intersection has to be found in order to exit the while loop.
        while (!queueA.isEmpty() && !queueB.isEmpty()) {
            if (queueA.size() + visitedA.size() < queueB.size() + visitedB.size()) {
                if (terminationStrategy.checkTermination(nodeDistA, estimatedDistA, queueA, nodeDistB, estimatedDistB, queueB, goalDistance)) {
                    break;
                }
                // Dijkstra from the 'From'-side
                Integer nextA = queueA.poll();
                if (nextA != null) {
                    visitedA.add(nextA);
                    for (Edge edge : adjList.get(nextA)) {
                        if (!visitedB.contains(edge.to)) {
                            relaxStrategy.relax(nextA, edge, A);
                            if (nodeDistA.get(nextA) + edge.d + nodeDistB.get(edge.to) < goalDistance) {
                                middlePoint = edge.to;
                                goalDistance = nodeDistA.get(nextA) + edge.d + nodeDistB.get(edge.to);
                            }
                        }
                    }
                }
            } else {
                if (terminationStrategy.checkTermination(nodeDistA, estimatedDistA, queueA, nodeDistB, estimatedDistB, queueB, goalDistance)) {
                    break;
                }
                // Dijkstra from the 'To'-side
                Integer nextB = queueB.poll();
                if (nextB != null) {
                    visitedB.add(nextB);
                    for (Edge edge : revAdjList.get(nextB)) {
                        if (!visitedA.contains(edge.to)) {
                            relaxStrategyB.relax(nextB, edge, B);
                            if (nodeDistB.get(nextB) + edge.d + nodeDistA.get(edge.to) < goalDistance) {
                                middlePoint = edge.to;
                                goalDistance = nodeDistB.get(nextB) + edge.d + nodeDistA.get(edge.to);
                            }
                        }
                    }
                }
            }

        }

        if (middlePoint == -1) {
            return new ShortestPathResult(Double.MAX_VALUE, new LinkedList<>(), 0);
        }
        visitedA.addAll(visitedB);
        List<Integer> shortestPath = extractPath(pathMapA, adjList, source, middlePoint);
        List<Integer> shortestPathB = extractPath(pathMapB, revAdjList, target, middlePoint);
        graph.reversePaintEdges(revAdjList, adjList);
        shortestPathB.remove(shortestPathB.size() - 1);
        Collections.reverse(shortestPathB);
        shortestPath.addAll(shortestPathB);
        double distance = goalDistance;
        return new ShortestPathResult(distance, shortestPath, visitedA.size());
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
        SSSP.distanceStrategy = distanceStrategy;
    }

    public static BiFunction<Node, Node, Double> getDistanceStrategy() {
        return distanceStrategy;
    }

    public static HeuristicFunction getHeuristicFunction() {
        return heuristicFunction;
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

    public static double[][] getLandmarkArray() {
        return landmarkArray;
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

    public static PriorityQueue<Integer> getQueue(ABDir dir) {
        return dir == A ? queueA : queueB;
    }

    public static Map<Integer, Double> getEstimatedDist(ABDir dir) {
        return dir == A ? estimatedDistA : estimatedDistB;
    }
}
