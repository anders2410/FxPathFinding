package paths.preprocessing;

import datastructures.JavaMinPriorityQueue;
import javafx.util.Pair;
import model.Edge;
import model.Graph;
import model.Node;
import paths.ABDir;

import java.util.*;
import java.util.function.BiConsumer;

import static paths.SSSP.getEdgeWeightStrategy;

/**
 * This class implements the pre-processing part of Contraction Hierarchies returning the augmented Graph and
 * the ranks of all the nodes.
 */
public class ContractionHierarchies {
    private final Graph graph;
    private final JavaMinPriorityQueue importanceQueue;

    // Instead of adding additional fields in Node we store values in internal lists
    private List<Boolean> contracted;
    private List<Integer> importance;
    private List<Integer> contractedNeighbours;
    private List<Integer> nodeLevel;
    private List<Integer> ranks;
    private List<Double> dijkstraDistanceList;
    private Set<Integer> dijkstraVisited;

    private Map<Integer, List<Integer>> inNodeMap;
    private Map<Pair<Integer, Integer>, Integer> shortcuts;

    private BiConsumer<Long, Long> progressListener = (l1, l2) -> { };

    public ContractionHierarchies(Graph graph) {
        this.graph = new Graph(graph);
        removeDuplicateEdges(this.graph);
        Comparator<Integer> comp = Comparator.comparingInt(i -> importance.get(i));
        importanceQueue = new JavaMinPriorityQueue(comp, graph.getNodeAmount());
        initializeLists();
        setInitialImportance();
    }

    // Initialize all the relevant lists, maps and sets.
    private void initializeLists() {
        contracted = new ArrayList<>();
        importance = new ArrayList<>();
        contractedNeighbours = new ArrayList<>();
        nodeLevel = new ArrayList<>();
        ranks = new ArrayList<>();
        shortcuts = new HashMap<>();
        dijkstraDistanceList = new ArrayList<>();
        dijkstraVisited = new HashSet<>();
        inNodeMap = getInNodeMap();

        for (Node ignored : graph.getNodeList()) {
            contracted.add(false);
            importance.add(0);
            contractedNeighbours.add(0);
            nodeLevel.add(0);
            ranks.add(0);
            dijkstraDistanceList.add(Double.MAX_VALUE);
        }
    }

    // Setting the initial importance of all the nodes
    private void setInitialImportance() {
        List<Node> nodeList = graph.getNodeList();
        for (Node n : nodeList) {
            importance.set(n.index, calculateImportance(n.index));
            importanceQueue.add(n.index);
        }
    }

    // The idea in the pre-processing step: We iterate the nodes one by one in the order of importance
    // and add 'shortcuts' whenever no witness path, for a given 'shortcut', has been found.
    public CHResult preprocess() {
        // Stores the number of nodes that are contracted
        int rank = 0;
        long numberOfContractedNodes = 0;

        while (!importanceQueue.isEmpty()) {
            int n = importanceQueue.poll();
            updateImportance(n);

            // If the vertex's recomputed importance is still minimum then contract it.
            // This is called a 'Lazy Update'.
            if (importanceQueue.size() != 0 && importance.get(n) > importance.get(importanceQueue.peek())) {
                importanceQueue.updatePriority(n);
                continue;
            }

            progressListener.accept(numberOfContractedNodes, (long) graph.getNodeAmount());

            ranks.set(n, rank);
            rank++;

            // Contraction part
            contractNode(n, false);
            numberOfContractedNodes++;
        }


        return new CHResult(graph, ranks, shortcuts);
    }

    // Function to contract a node. If computeNumberOfShortcuts is true it will not add the actual shortcuts,
    // but rather return a value telling how many it would have contracted.
    private int contractNode(int n, boolean computeNumberOfShortcuts) {
        int numberOfShortcuts = 0;

        if (!computeNumberOfShortcuts) {
            // Set contracted == true for the current node.
            contracted.set(n, true);
            // Update the given node's neighbors about that the given node has been contracted.
            updateNeighbours(n);
        }

        // Find the lists of incoming and outgoing edges/nodes
        List<Edge> inEdgeList = getInEdgeList(inNodeMap.get(n), n);
        List<Edge> outEdgeList = graph.getAdjList().get(n);

        // Stores the max distance out of uncontracted in- & outNodes of the given Node.
        double inMax = 0;
        double outMax = 0;

        // Find inMax;
        for (Edge inEdge : inEdgeList) {
            if (contracted.get(inEdge.from)) {
                continue;
            }
            if (inMax < getEdgeWeightStrategy().getWeight(inEdge, ABDir.A)) {
                inMax = getEdgeWeightStrategy().getWeight(inEdge, ABDir.A);
            }
        }

        // Find outMax
        for (Edge outEdge : outEdgeList) {
            if (contracted.get(outEdge.to)) {
                continue;
            }
            if (outMax < getEdgeWeightStrategy().getWeight(outEdge, ABDir.A)) {
                outMax = getEdgeWeightStrategy().getWeight(outEdge, ABDir.A);
            }
        }

        double max = inMax + outMax;

        // Iterating over all the incoming nodes
        for (Edge inEdge : inEdgeList) {
            int inNodeIndex = inEdge.from;
            double inCost = getEdgeWeightStrategy().getWeight(inEdge, ABDir.A);

            // If the node has already been contracted we will ignore it.
            if (contracted.get(inNodeIndex)) {
                continue;
            }

            // Finds the shortest distances from the inNode to all the outNodes.
            List<Double> distanceList = dijkstra(n, inNodeIndex, max);

            // This adds shortcuts if no witness path was found.
            for (Edge outEdge : outEdgeList) {
                int outNodeIndex = outEdge.to;
                double outCost = getEdgeWeightStrategy().getWeight(outEdge, ABDir.A);

                // If the node has already been contracted we will ignore it.
                if (contracted.get(outNodeIndex) || inNodeIndex == outNodeIndex) {
                    continue;
                }

                double totalCost = Double.sum(inCost, outCost);

                // Checks if a witness path exists. If it doesnt we will add a shortcut bypassing node n.
                if (distanceList.get(outNodeIndex) > totalCost) {
                    // A switch depending on whether it should only calculate how many shortcuts.
                    if (computeNumberOfShortcuts) {
                        numberOfShortcuts++;
                    } else {
                        boolean alreadyHasEdge = false;
                        Pair<Edge, Integer> alreadyEdge = new Pair<>(new Edge(0, 0, Double.MAX_VALUE), 0);
                        List<Edge> get = graph.getAdjList().get(inNodeIndex);
                        for (int i = 0, getSize = get.size(); i < getSize; i++) {
                            Edge e = get.get(i);
                            if (e.to == outNodeIndex) {
                                alreadyHasEdge = true;
                                alreadyEdge = new Pair<>(e, i);
                            }
                        }

                        if (alreadyHasEdge) {
                            if (getEdgeWeightStrategy().getWeight(alreadyEdge.getKey(), ABDir.A) > totalCost) {
                                graph.getAdjList().get(inNodeIndex).set(alreadyEdge.getValue(), new Edge(inNodeIndex, outNodeIndex, totalCost));

                                Pair<Integer, Integer> pair3 = new Pair<>(inNodeIndex, outNodeIndex);
                                if (shortcuts.get(pair3) != null) {
                                    shortcuts.replace(pair3, n);
                                } else {
                                    shortcuts.put(pair3, n);
                                }
                            }
                        } else {
                            graph.addEdge(inNodeIndex, outNodeIndex, totalCost);

                            List<Integer> temp11 = inNodeMap.get(outNodeIndex);
                            temp11.add(inNodeIndex);
                            inNodeMap.replace(outNodeIndex, temp11);

                            Pair<Integer, Integer> pair3 = new Pair<>(inNodeIndex, outNodeIndex);
                            shortcuts.put(pair3, n);
                        }
                    }
                }
            }
        }
        return numberOfShortcuts;
    }

    // Is used in the witness search. It is a standard Dijkstra implementation where, the node we are about to
    // contract, is excluded from the search. As we want to find the shortest path bypassing it, if it exist.
    private List<Double> dijkstra(int contractedNode, int inNode, double maxCost) {
        for (Integer i : dijkstraVisited) {
            dijkstraDistanceList.set(i, Double.MAX_VALUE);
        }
        dijkstraVisited.clear();

        List<Double> distanceList = dijkstraDistanceList;

        Comparator<Integer> comp = getComparator();
        JavaMinPriorityQueue queue = new JavaMinPriorityQueue(comp, graph.getNodeAmount());

        distanceList.set(inNode, 0.0);

        queue.add(inNode);

        int i = 0;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            dijkstraVisited.add(node);

            if (i > 5 || distanceList.get(node) > maxCost) {
                return distanceList;
            }

            for (Edge edge : graph.getAdjList().get(node)) {
                int temp = edge.to;
                double cost = getEdgeWeightStrategy().getWeight(edge, ABDir.A);

                if (contracted.get(temp) || contractedNode == temp) {
                    continue;
                }

                if (distanceList.get(temp) > distanceList.get(node) + cost) {
                    distanceList.set(temp, distanceList.get(node) + cost);
                    queue.updatePriority(temp);
                    dijkstraVisited.add(temp);
                }
            }
            i++;
        }
        return distanceList;
    }

    private Comparator<Integer> getComparator() {
        return (i, j) -> {
            double diff = Math.abs(dijkstraDistanceList.get(i) - dijkstraDistanceList.get(j));
            if (diff <= 0.000000000000001) {
                return i.compareTo(j);
            } else {
                return Double.compare(dijkstraDistanceList.get(i), dijkstraDistanceList.get(j));
            }
        };
    }

    // --------------------------------------------- IMPORTANCE ------------------------------------------

    /**
     * The edgeDifference is defined by edgeDifference = s(n) - in(n) - out(n). Where s(n) is the number of
     * added shortcuts, in(n) is in degree and out(n) is out degree. We want to contract nodes
     * with a small edgeDifference.
     */
    private int edgeDifference(int n) {
        int inDegree = inNodeMap.get(n).size();
        int outDegree = graph.getAdjList().get(n).size();
        int numberOfShortcuts = contractNode(n, true);
        return numberOfShortcuts - inDegree - outDegree;
    }

    /**
     * We are interested in spreading out the contracted nodes across the graph so they
     * do not cluster together. We will contract nodes with a small number of already contracted
     * neighbours. Defined as 'Deleted Neighbours' by Sanders & Schultes.
     */
    private int contractedNeighbours(int n) {
        return contractedNeighbours.get(n);
    }

    /**
     * The Node Level L(n) is an upper bound on the number of edges in the shortest path from any
     * s to n in the augmented path. Initially, L(n) is 0. After contracting node n, for neighbours u
     * of n do L(u) <- max(L(u), L(n) + 1). We contract a node with small L(n).
     */
    private int nodeLevel(int n) {
        return nodeLevel.get(n);
    }

    /**
     * The importance is used when constructing the augmented graph. We want to contract all the
     * nodes with low importance first, and then gradually work our way through the graph using
     * a priorityQueue based on the importance.
     * We can optionally experiment with some different weight to all the functions.
     */
    private int calculateImportance(int n) {
        return edgeDifference(n) + contractedNeighbours(n) + nodeLevel(n);
    }

    // Update the importance of a node
    private void updateImportance(int n) {
        importance.set(n, calculateImportance(n));
    }

    // ----------------------------- UTILITIES ------------------------------------------
    // Calculates the incoming nodes
    private Map<Integer, List<Integer>> getInNodeMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        List<Node> nodeList = graph.getNodeList();
        for (int i = 0; i < nodeList.size(); i++) {
            for (Edge e : graph.getAdjList().get(i)) {
                List<Integer> list = map.computeIfAbsent(e.to, k -> new ArrayList<>());
                list.add(nodeList.get(i).index);
                map.replace(e.to, list);
            }
        }
        return map;
    }

    // Get all incoming edges for a given node.
    private List<Edge> getInEdgeList(List<Integer> inNodeList, int node) {
        List<Edge> inEdgeList = new ArrayList<>();
        for (int inNode : inNodeList) {
            for (Edge e : graph.getAdjList().get(inNode)) {
                if (e.to == node) {
                    inEdgeList.add(e);
                    break;
                }
            }
        }
        return inEdgeList;
    }

    // Find all neighbours of a given node.
    private List<Integer> getNeighbours(int node) {
        List<Integer> list = new ArrayList<>();
        for (Edge e : graph.getAdjList().get(node)) {
            list.add(e.to);
        }
        list.addAll(inNodeMap.get(node));
        return new ArrayList<>(new HashSet<>(list));
    }

    // Update the neighbours of the contracted node that this node has been contracted.
    private void updateNeighbours(int n) {
        for (Integer neighbour : getNeighbours(n)) {
            contractedNeighbours.set(neighbour, contractedNeighbours.get(neighbour) + 1);
            nodeLevel.set(neighbour, Math.max(nodeLevel.get(neighbour), nodeLevel.get(n) + 1));
        }

        // Update the neighbours in Priority Queue.
        // Another update heuristic to ensure that the priority queue is correct!
        for (Integer neighbour : getNeighbours(n)) {
            if (!contracted.get(neighbour)) {
                updateImportance(neighbour);
                importanceQueue.updatePriority(neighbour);
            }
        }
    }

    // Remove duplicate edges. Always select the one with lowest cost.
    // TODO: 28/04/2020 Consider if this should be done in collapsing?
    private void removeDuplicateEdges(Graph graph) {
        for (int i = 0; i < graph.getNodeList().size(); i++) {
            Iterator<Edge> iterator = graph.getAdjList().get(i).iterator();
            List<Edge> copyEdges = new ArrayList<>(graph.getAdjList().get(i));
            while (iterator.hasNext()) {
                Edge e = iterator.next();
                for (Edge copyEdge : copyEdges) {
                    if (copyEdge.d < e.d && copyEdge.to == e.to) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        // Removing all self-referring edges in the Graph
        // TODO: 17/04/2020 Fix hack with removing self-referring edges
        for (Node nodeHack : graph.getNodeList()) {
            List<Edge> edgeList = graph.getAdjList().get(nodeHack.index);
            edgeList.removeIf(edge -> nodeHack.index == edge.to);
        }
    }

    public void setProgressListener(BiConsumer<Long, Long> progressListener) {
        this.progressListener = progressListener;
    }

    /* // Remove duplicate edges. Always select the one with lowest cost.
    private void removeDuplicateEdges(Graph graph) {
        for (int i = 0; i < graph.getNodeList().size(); i++) {
            Set<Edge> set = new HashSet<>();
            List<Edge> copyEdges = new ArrayList<>(graph.getAdjList().get(i));
            for (Edge e : graph.getAdjList().get(i)) {
                for (Edge copyEdge : copyEdges) {
                    if (copyEdge.d <= e.d && copyEdge.to == e.to) {
                        set.remove(e);
                        set.add(copyEdge);
                    }
                }
            }
            graph.getAdjList().set(i, new ArrayList<>(set));
        }

        for (Node nodeHack : graph.getNodeList()) {
            List<Edge> edgeList = graph.getAdjList().get(nodeHack.index);
            edgeList.removeIf(edge -> nodeHack.index == edge.to);
        }
    }*/

    /*// --------------------------------------- BIDIRECTIONAL ----------------------------------------
    // This is now all obsolete as it has been integrated into SSSP!
    public Pair<Double, Set<Integer>> computeDist(Graph CHGraph, int source, int target) {
        List<Double> forwardDistanceList = new ArrayList<>();
        List<Double> reverseDistanceList = new ArrayList<>();

        for (int i = 0; i < CHGraph.getNodeAmount(); i++) {
            forwardDistanceList.add(Double.MAX_VALUE);
            reverseDistanceList.add(Double.MAX_VALUE);
        }

        List<List<Edge>> adjList = CHGraph.getAdjList();
        List<List<Edge>> revAdjList = CHGraph.getReverse(adjList);

        forwardDistanceList.set(source, 0.0);
        reverseDistanceList.set(target, 0.0);

        Set<Integer> processedForward = new HashSet<>();
        Set<Integer> processedReverse = new HashSet<>();

        Comparator<Integer> compForward = Comparator.comparingDouble(forwardDistanceList::get);
        Comparator<Integer> compReverse = Comparator.comparingDouble(reverseDistanceList::get);
        JavaMinPriorityQueue forwardQ = new JavaMinPriorityQueue(compForward, CHGraph.getNodeAmount());
        JavaMinPriorityQueue reverseQ = new JavaMinPriorityQueue(compReverse, CHGraph.getNodeAmount());

        Map<Integer, Integer> backpointersForward = new HashMap<>();
        Map<Integer, Integer> backpointersReverse = new HashMap<>();

        forwardQ.add(source);
        reverseQ.add(target);

        while (!forwardQ.isEmpty() || !reverseQ.isEmpty()) {
            if (!forwardQ.isEmpty()) {
                int nodeForward = forwardQ.poll();
                processedForward.add(nodeForward);

                for (Edge edge : adjList.get(nodeForward)) {
                    int temp = edge.to;
                    double cost = edge.d;
                    if (forwardDistanceList.get(nodeForward) + cost < forwardDistanceList.get(temp) && ranks.get(nodeForward) < ranks.get(temp)) {
                        forwardDistanceList.set(temp, forwardDistanceList.get(nodeForward) + cost);
                        forwardQ.updatePriority(temp);
                        backpointersForward.put(temp, nodeForward);
                    }
                }
            }

            if (!reverseQ.isEmpty()) {
                int nodeReverse = reverseQ.poll();
                processedReverse.add(nodeReverse);

                for (Edge edge : revAdjList.get(nodeReverse)) {
                    int temp = edge.to;
                    double cost = edge.d;

                    if (reverseDistanceList.get(nodeReverse) + cost < reverseDistanceList.get(temp) && ranks.get(nodeReverse) < ranks.get(temp)) {
                        reverseDistanceList.set(temp, reverseDistanceList.get(nodeReverse) + cost);
                        reverseQ.updatePriority(temp);
                        backpointersReverse.put(temp, nodeReverse);
                    }
                }
            }
        }

        // Goes through all overlapping nodes and find the one with the smallest distance.
        int middlepoint = -1;
        double finalDistance = Double.MAX_VALUE;
        for (int node : processedForward) {
            if (processedForward.contains(node) && processedReverse.contains(node)) {
                // Replace if lower than actual
                double distance = forwardDistanceList.get(node) + reverseDistanceList.get(node);
                if (0 <= distance && distance < finalDistance) {
                    finalDistance = distance;
                    middlepoint = node;
                }
            }
        }

        List<Integer> shortestPathA = extractPath(backpointersForward, source, middlepoint);
        List<Integer> shortestPathB = extractPath(backpointersReverse, target, middlepoint);
        shortestPathB.remove(shortestPathB.size() - 1);
        Collections.reverse(shortestPathB);
        shortestPathA.addAll(shortestPathB);

        *//*Set<Integer> result = new LinkedHashSet<>();
        for (int i = 0; i < shortestPathA.size() - 1; i++) {
            List<Integer> contractedNodes = shortcuts.get(new Pair<>(shortestPathA.get(i), shortestPathA.get(i + 1)));
            result.add(shortestPathA.get(i));
            if (contractedNodes != null) {
                result.addAll(contractedNodes);
            }
            result.add(shortestPathA.get(i + 1));
        }*//*

        return new Pair<>(finalDistance, new HashSet<>());
    }

    private List<Integer> extractPath(Map<Integer, Integer> pathMap, int from, int to) {
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
    }*/
}

