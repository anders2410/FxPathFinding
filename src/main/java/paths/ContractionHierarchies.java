package paths;

import datastructures.JavaMinPriorityQueue;
import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;

/**
 * This class implements the algorithm behind Contraction Hierarchies. Both doing the augmented graph
 * and the Bi-directional Dijkstras search on it.
 */
public class ContractionHierarchies {
    private Graph graph;

    private JavaMinPriorityQueue importanceQueue;

    // Instead of adding additional fields in Node we store values in internal lists
    private List<Boolean> contracted;
    private List<Integer> importance;
    private List<Integer> contractedNeighbours;
    private List<Integer> ranks;
    private List<Double> dijkstraDistanceList;
    private Set<Integer> dijkstraVisited;

    private Map<Integer, List<Integer>> inNodeMap;

    public ContractionHierarchies(Graph graph) {
        this.graph = graph;
        Comparator<Integer> comp = Comparator.comparingInt(i -> importance.get(i));
        importanceQueue = new JavaMinPriorityQueue(comp, graph.getNodeAmount());
        initializeLists();
        setInitialImportance();
    }

    private void initializeLists() {
        contracted = new ArrayList<>();
        importance = new ArrayList<>();
        contractedNeighbours = new ArrayList<>();
        ranks = new ArrayList<>();
        dijkstraDistanceList = new ArrayList<>();
        dijkstraVisited = new HashSet<>();

        inNodeMap = getInNodeMap();

        for (Node ignored : graph.getNodeList()) {
            contracted.add(false);
            importance.add(0);
            contractedNeighbours.add(0);
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

    // We iterate the nodes one by one in the order of importance and add 'Shortcuts'
    // whenever no witness path has been found.
    public Graph preprocess() {
        // Stores the number of nodes that are contracted
        int rank = 0;

        while (!importanceQueue.isEmpty()) {
            int n = importanceQueue.poll();
            updateImportance(n);

            // If the vertex's recomputed importance is still minimum then contract it.
            // This is called 'Lazy Update'
            if (importanceQueue.size() != 0 && importance.get(n) > importance.get(importanceQueue.peek())) {
                importanceQueue.updatePriority(n);
                continue;
            }

            ranks.set(n, rank);
            rank++;

            // Contraction part
            // System.out.println(importance.get(n));
            contractNode(n);
        }

        // TODO: 17/04/2020 Fix hack with removing self-referring edges
        for (Node nodeHack : graph.getNodeList()) {
            List<Edge> edgeList = graph.getAdjList().get(nodeHack.index);
            edgeList.removeIf(edge -> nodeHack.index == edge.to);
        }

        return graph;
    }

    // Function to contract a node!
    private void contractNode(int n) {
        // Set contracted == true for the current node.
        contracted.set(n, true);

        // Update the given node's neighbors about that the given node has been contracted.
        updateNeighbours(n);

        // Find the lists of incoming and outgoing edges/nodes
        List<Integer> inNodeList = inNodeMap.get(n);
        List<Edge> outEdgeList = graph.getAdjList().get(n);

        // Stores the max distance out of uncontracted inVertices of the given vertex.
        double inMax = 0;
        // Stores the max distance out of uncontracted outVertices of the given vertex.
        double outMax = 0;

        // Find inMax;
        for (int i = 0; i < inNodeList.size(); i++) {
            if (contracted.get(inNodeList.get(i))) {
                continue;
            }
            if (inMax < getInCost(n, i)) {
                inMax = getInCost(n, i);
            }
        }

        // Find outMax
        for (Edge edge : outEdgeList) {
            if (contracted.get(edge.to)) {
                continue;
            }
            if (outMax < edge.d) {
                outMax = edge.d;
            }
        }

        double max = inMax + outMax;

        // Iterating over all the incoming nodes
        for (Integer inNode : inNodeList) {
            int inNodeIndex = inNode;

            // If the node has already been contracted we will ignore it.
            if (contracted.get(inNodeIndex)) {
                continue;
            }

            // Find the inCost of an edge.
            double inCost = getInCost(n, inNodeIndex);

            // Finds the shortest distances from the inNode to all the outNodes.
            List<Double> distanceList = dijkstra(inNodeIndex, max);

            // This adds shortcuts if no witness path was found.
            for (Edge outEdge : outEdgeList) {
                int outNodeIndex = outEdge.to;
                double outCost = outEdge.d;

                // If the node has already been contracted we will ignore it.
                if (contracted.get(outNodeIndex)) {
                    continue;
                }

                double totalCost = inCost + outCost;

                // Checks if a witness path exists. If it doesnt we will add a shortcut bypassing node n.
                if (distanceList.get(outNodeIndex) > totalCost) {
                    // TODO: 15/04/2020 Add implementation for one-way streets
                    graph.addEdge(inNodeIndex, outNodeIndex, totalCost);

                    List<Integer> temp1 = inNodeMap.get(outNodeIndex);
                    temp1.add(inNodeIndex);
                    inNodeMap.replace(outNodeIndex, temp1);

                    if (checkIfOutNodeShortcut(n, inNodeIndex, outNodeIndex)) {
                        graph.addEdge(outNodeIndex, inNodeIndex, totalCost);

                        List<Integer> temp2 = inNodeMap.get(inNodeIndex);
                        temp2.add(outNodeIndex);
                        inNodeMap.replace(inNodeIndex, temp2);
                    }
                }
            }
        }
    }

    private boolean checkIfOutNodeShortcut(int node, int inNode, int outNode) {
        boolean something = false;
        for (Edge e : graph.getAdjList().get(node)) {
            if (e.to == inNode) {
                something = true;
                break;
            }
        }

        boolean anything = false;
        for (Integer n : inNodeMap.get(node)) {
            if (n == outNode) {
                anything = true;
                break;
            }
        }

        return something && anything;
    }

    private List<Double> dijkstra(int inNode, double maxCost) {
        for (Integer i : dijkstraVisited) {
            dijkstraDistanceList.set(i, Double.MAX_VALUE);
        }

        dijkstraVisited.clear();

        List<Double> distanceList = dijkstraDistanceList;

        Comparator<Integer> comp = Comparator.comparingDouble(distanceList::get);
        JavaMinPriorityQueue queue = new JavaMinPriorityQueue(comp, graph.getNodeAmount());

        distanceList.set(inNode, 0.0);

        queue.clear();
        queue.add(inNode);

        int i = 0;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            dijkstraVisited.add(node);

            if (i > 3 || distanceList.get(node) > maxCost) {
                return distanceList;
            }

            for (Edge edge : graph.getAdjList().get(node)) {
                int temp = edge.to;
                double cost = edge.d;

                if (contracted.get(temp) || node == temp) {
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

    // Calculate the cost of a given incoming edge/node. (inNode -> n)
    private double getInCost(int source, int inNode) {
        double inCost = 0;
        for (Edge e : graph.getAdjList().get(inNode)) {
            if (e.to == source) {
                inCost = e.d;
                break;
            }
        }
        return inCost;
    }

    // Update the importance of a node
    private void updateImportance(int n) {
        importance.set(n, calculateImportance(n));
    }

    /**
     * The edgeDifference is defined by edgeDifference = s(n) - in(n) - out(n). Where s(n) is the number of
     * added shortcuts, in(n) is in degree and out(n) is out degree. We want to contract nodes
     * with a small edgeDifference.
     */
    private int edgeDifference(int n) {
        int inDegree = inNodeMap.get(n).size();
        int outDegree = graph.getAdjList().get(n).size();
        int numberOfShortcuts = inDegree * outDegree;
        return numberOfShortcuts - inDegree + outDegree;
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
        return 0;
    }

    /**
     * The importance is used when constructing the augmented graph. We want to contract all the
     * nodes with low importance first, and then gradually work our way through the graph using
     * a priorityQueue based on the importance.
     * <p>
     * We can optionally experiment with some different weight to all the functions.
     */
    private int calculateImportance(int n) {
        return edgeDifference(n) + contractedNeighbours(n);
    }

    // Update the neighbours of the contracted node that this node has been contracted.
    private void updateNeighbours(int n) {
        List<Edge> adj = graph.getAdjList().get(n);
        for (Edge edge : adj) {
            contractedNeighbours.set(edge.to, contractedNeighbours.get(edge.to) + 1);
        }

        List<Integer> inDegreeList = inNodeMap.get(n);
        for (Integer node : inDegreeList) {
            contractedNeighbours.set(node, contractedNeighbours.get(node) + 1);
        }

        // Update the neighbours
        for (int neighbour : getNeighbours(n)) {
            if (!contracted.get(neighbour)) {
                updateImportance(neighbour);
                importanceQueue.updatePriority(neighbour);
            }
        }
    }

    // --------------------------------------- BIDIRECTIONAL ----------------------------------------
    public double computeDist(Graph CHGraph, int source, int target) {
        List<Double> forwardDistanceList = new ArrayList<>();
        List<Double> reverseDistanceList = new ArrayList<>();

        for (int i = 0; i < CHGraph.getNodeAmount(); i++) {
            forwardDistanceList.add(Double.MAX_VALUE);
            reverseDistanceList.add(Double.MAX_VALUE);
        }

        forwardDistanceList.set(source, 0.0);
        reverseDistanceList.set(target, 0.0);

        Set<Integer> processedForward = new HashSet<>();
        Set<Integer> processedReverse = new HashSet<>();

        Comparator<Integer> compForward = Comparator.comparingDouble(forwardDistanceList::get);
        Comparator<Integer> compReverse = Comparator.comparingDouble(reverseDistanceList::get);
        JavaMinPriorityQueue forwardQ = new JavaMinPriorityQueue(compForward, CHGraph.getNodeAmount());
        JavaMinPriorityQueue reverseQ = new JavaMinPriorityQueue(compReverse, CHGraph.getNodeAmount());

        forwardQ.add(source);
        reverseQ.add(target);

        while (!forwardQ.isEmpty() || !reverseQ.isEmpty()) {
            if (!forwardQ.isEmpty()) {
                int nodeForward = forwardQ.poll();
                processedForward.add(nodeForward);

                for (Edge edge : CHGraph.getAdjList().get(nodeForward)) {
                    int temp = edge.to;
                    double cost = edge.d;

                    if (forwardDistanceList.get(nodeForward) + cost < forwardDistanceList.get(temp) && ranks.get(nodeForward) < ranks.get(temp)) {
                        forwardDistanceList.set(temp, forwardDistanceList.get(nodeForward) + cost);
                        forwardQ.updatePriority(temp);
                    }
                }
            }

            if (!reverseQ.isEmpty()) {
                int nodeReverse = reverseQ.poll();
                processedReverse.add(nodeReverse);

                for (Edge edge : CHGraph.getAdjList().get(nodeReverse)) {
                    int temp = edge.to;
                    double cost = edge.d;

                    if (reverseDistanceList.get(nodeReverse) + cost < reverseDistanceList.get(temp) && ranks.get(nodeReverse) < ranks.get(temp)) {
                        reverseDistanceList.set(temp, reverseDistanceList.get(nodeReverse) + cost);
                        reverseQ.updatePriority(temp);
                    }
                }
            }
        }

        processedForward.retainAll(processedReverse);
        System.out.println(processedForward.toString());

        double estimate = Double.MAX_VALUE;
        for (int node : processedForward) {
            if(forwardDistanceList.get(node) + reverseDistanceList.get(node) < estimate) {
                estimate = forwardDistanceList.get(node) + reverseDistanceList.get(node);
            }
        }

        return estimate;
    }

    // ----------------------------- UTILITIES ------------------------------------------

    // Find the neighbours of a given node.
    private List<Integer> getNeighbours(int node) {
        List<Integer> list = new ArrayList<>();
        for (Edge e : graph.getAdjList().get(node)) {
            list.add(e.to);
        }
        list.addAll(inNodeMap.get(node));
        return list;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }
}
