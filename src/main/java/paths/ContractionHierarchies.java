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
    private Graph augmentedGraph;

    private JavaMinPriorityQueue importanceQueue;
    private Map<Integer, List<Node>> inDegreeMap;

    private GraphUtil graphUtil;

    // Instead of adding additional fields in Node we store values in internal lists
    private List<Boolean> contracted;
    private List<Integer> importance;
    private List<Integer> contractedNeighbours;
    private List<Integer> ranks;
    private List<Distance> distances;

    public ContractionHierarchies(Graph graph) {
        this.graph = graph;
        // The Graph with augmented edges (added all shortcuts)
        augmentedGraph = new Graph(graph);

        importanceQueue = new JavaMinPriorityQueue(getImportanceComparator(), graph.getNodeAmount());

        graphUtil = new GraphUtil(graph);
        // A map to find all inNodes for a given node.
        inDegreeMap = graphUtil.getInDegreeNodeMap();

        initializeLists();
        setInitialImportance();
    }

    private void initializeLists() {
        contracted = new ArrayList<>();
        importance = new ArrayList<>();
        contractedNeighbours = new ArrayList<>();
        ranks = new ArrayList<>();
        distances = new ArrayList<>();

        for (Node ignored : graph.getNodeList()) {
            contracted.add(false);
            importance.add(0);
            contractedNeighbours.add(0);
            ranks.add(0);
            distances.add(new Distance());
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

    // We iterate the nodes one by one in the order of importance and add 'Shortcuts'
    // whenever no witness path has been found.
    public Graph preprocess() {
        // Contains the vertices in the order they are contracted
        int[] nodeOrdering = new int[graph.getNodeAmount()];
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

            nodeOrdering[rank] = n;
            ranks.set(n, rank);
            rank++;

            // Contraction part
            //System.out.println(importance.get(n));
            contractNode(n, rank - 1);
            //contractNodeAlt(n);
        }

        return augmentedGraph;
    }

    private List<Integer> getNeighbours(int node) {
        List<Integer> list = new ArrayList<>();
        for (Edge e : graph.getAdjList().get(node)) {
            list.add(e.to);
        }
        for (Node n : inDegreeMap.get(node)) {
            list.add(n.index);
        }
        return list;
    }

    // Function to contract a node!
    private void contractNode(int n, int contractID) {
        // Set contracted == true for the current node.
        contracted.set(n, true);

        // Update the given node's neighbors about that the given node has been contracted.
        updateNeighbours(n);

        // Find the lists of incoming and outgoing edges/nodes
        List<Node> inNodeList = inDegreeMap.get(n);
        List<Edge> outEdgeList = graph.getAdjList().get(n);

        // Stores the max distance out of uncontracted inVertices of the given vertex.
        double inMax = 0;
        // Stores the max distance out of uncontracted outVertices of the given vertex.
        double outMax = 0;

        // Find inMax;
        for (int i = 0; i < inNodeList.size(); i++) {
            if (contracted.get(inNodeList.get(i).index)) {
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
        for (int i = 0; i < inNodeList.size(); i++) {
            int inNode = inNodeList.get(i).index;

            // If the node has already been contracted we will ignore it.
            if (contracted.get(inNode)) {
                continue;
            }

            // Find the inCost of an edge.
            double inCost = getInCost(n, inNode);

            // Finds the shortest distances from the inNode to all the outNodes.
            List<Double> distanceList = dijkstra(inNode, max);

            // This adds shortcuts if no witness path was found.
            for (Edge outEdge : outEdgeList) {
                int outNode = outEdge.to;
                double outCost = outEdge.d;

                // If the node has already been contracted we will ignore it.
                if (contracted.get(outNode) || inNode == outNode) {
                    continue;
                }

                double totalCost = inCost + outCost;
                //System.out.println(inNodeList);
                //System.out.println(outEdgeList);
                //System.out.println("Length from " + inNode + " to " + outNode + ": " + distances.get(outNode).distance);
                //System.out.println("Length through " + n + ": " + totalCost);

                // Checks if a witness path exists. If it doesnt we will add a shortcut bypassing node n.
                if (distanceList.get(outNode) > totalCost) {
                    // TODO: 15/04/2020 Add implementation for one-way streets
                    augmentedGraph.addEdge(inNode, outNode, totalCost);
                    augmentedGraph.addEdge(outNode, inNode, totalCost);
                    //System.out.println("Shortcut added!");
                }
            }
        }

        // Update the neighbours
        for (Edge neighbour : graph.getAdjList().get(n)) {
            if (!contracted.get(neighbour.to)) {
                updateImportance(neighbour.to);
                importanceQueue.updatePriority(neighbour.to);
            }
        }
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

    private List<Double> dijkstra(int inNode, double maxCost) {
        List<Double> distanceList = new ArrayList<>();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            distanceList.add(Double.MAX_VALUE);
        }

        Comparator<Integer> comp = Comparator.comparingDouble(distanceList::get);
        JavaMinPriorityQueue queue = new JavaMinPriorityQueue(comp, graph.getNodeAmount());

        distanceList.set(inNode, 0.0);

        queue.clear();
        queue.add(inNode);

        int i = 0;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            if (i > 3 || distances.get(node).distance > maxCost) {
                break;
            }
            relaxEdges(node, queue);
            i++;
        }

        return distanceList;
    }

    private void relaxEdges(int node, JavaMinPriorityQueue queue) {
        List<Edge> outEdgeList = graph.getAdjList().get(node);

        for (Edge edge : outEdgeList) {
            int temp = edge.to;
            double cost = edge.d;

            if (contracted.get(temp)) {
                continue;
            }

            if (!(node == temp) && distances.get(temp).distance > distances.get(node).distance + cost) {
                distances.get(temp).distance = distances.get(node).distance + cost;

                queue.updatePriority(temp);
            }
        }
    }

    /*private void contractNodeAlt(int node) {
        // Set contracted == true for the current node.
        contracted.set(node, true);
        // Update the given node's neighbors about that the given node has been contracted.
        updateNeighbours(node);

        Map<NeighbourPair, Double> neighbourPairDistances = calculateMinimalDistanceBetweenNeighborsViaNode(node);
        List<Node> neighbours = inDegreeMap.get(node);
        for (Node neighbour : neighbours) {
            List<Double> shortestPathDistances = new ArrayList<>();
            for (Node ignored : graph.getNodeList()) {
                shortestPathDistances.add(Double.MAX_VALUE);
            }

            Comparator<Integer> comp = Comparator.comparingDouble(shortestPathDistances::get);
            JavaMinPriorityQueue queue = new JavaMinPriorityQueue(comp, graph.getNodeAmount());

            shortestPathDistances.set(neighbour.index, 0.0);
            queue.clear();
            queue.add(neighbour.index);

            while (!queue.isEmpty()) {
                int n = queue.poll();
                if (shortestPathDistances.get(n) > getUpperBound(neighbourPairDistances)) {
                    break;
                }
                for (Edge e : graph.getAdjList().get(n)) {
                    if (!(n == node) && shortestPathDistances.get(n) + getDistance(n, e.to) < shortestPathDistances.get(e.to)) {
                        shortestPathDistances.set(e.to, shortestPathDistances.get(n) + getDistance(n, e.to));
                        queue.updatePriority(e.to);
                    }
                }
            }

            for (Edge n : graph.getAdjList().get(node)) {
                NeighbourPair neighbourPair = new NeighbourPair(neighbour.index, n.to);
                //System.out.println(neighbourPairDistances.getOrDefault(neighbourPair, Double.MAX_VALUE));
                //System.out.println(neighbourPairDistances.keySet());
                if (shortestPathDistances.get(n.to) > neighbourPairDistances.getOrDefault(neighbourPair, Double.MAX_VALUE)) {
                    augmentedGraph.addEdge(neighbourPair.getInNeighbour(), neighbourPair.getOutNeighbour(), neighbourPairDistances.get(neighbourPair));
                    augmentedGraph.addEdge(neighbourPair.getOutNeighbour(), neighbourPair.getInNeighbour(), neighbourPairDistances.get(neighbourPair));

                }
            }
        }

        // Update the neighbours
        for (Edge neighbour : graph.getAdjList().get(node)) {
            if (!contracted.get(neighbour.to)) {
                updateImportance(neighbour.to);
                importanceQueue.updatePriority(neighbour.to);
            }
        }
    }

    private Double getDistance(int n, int nb) {
        for (Edge e : graph.getAdjList().get(n)) {
            if (e.to == nb) {
                return e.d;
            }
        }
        return Double.MAX_VALUE;
    }

    private Double getUpperBound(Map<NeighbourPair, Double> neighbourPairDistances) {
        double max = 0;
        for (double d : neighbourPairDistances.values()) {
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    private Map<NeighbourPair, Double> calculateMinimalDistanceBetweenNeighborsViaNode(int node) {
        Map<NeighbourPair, Double> temp = new HashMap<>();
        for (Node n : inDegreeMap.get(node)) {
            for (Edge e : graph.getAdjList().get(node)) {
                if (n.index == e.to) { continue; }
                temp.put(new NeighbourPair(n.index, e.to), getInCost(node, n.index) + e.d);
            }
        }
        return temp;
    }*/


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
        int inDegree = inDegreeMap.get(n).size();
        int outDegree = graphUtil.getOutDegree(n);
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
     * The idea is that we want to contract important nodes later. The shortcutCover sc(n) is the
     * number of neighbours m of n such that we could have a shortcut to or from m after contracting n.
     * If the sc(n) is big, many nodes depend on n. We will contract a node with small sc(n).
     */
    private int shortcutCover(int n) {
        return graph.getAdjList().get(n).size() + inDegreeMap.get(n).size();
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
        return edgeDifference(n) * 14 + contractedNeighbours(n) * 25 + nodeLevel(n);
    }

    // Update the neighbours of the contracted node that this node has been contracted.
    private void updateNeighbours(int n) {
        List<Edge> adj = graph.getAdjList().get(n);
        for (Edge edge : adj) {
            contractedNeighbours.set(edge.to, contractedNeighbours.get(edge.to) + 1);
        }

        List<Node> inDegreeList = inDegreeMap.get(n);
        for (Node node : inDegreeList) {
            contractedNeighbours.set(node.index, contractedNeighbours.get(node.index) + 1);
        }
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    // Set the queue to compare on Importance
    private Comparator<Integer> getImportanceComparator() {
        return Comparator.comparingInt(i -> importance.get(i));
    }

    // Set the queue to compare on Distance
    private Comparator<Integer> getPriorityComparator() {
        return Comparator.comparingDouble(i -> distances.get(i).distance);
    }

    // --------------------------------------- BIDIRECTIONAL ----------------------------------------
    JavaMinPriorityQueue forwardQ;
    JavaMinPriorityQueue reverseQ;

    List<Double> forwardDistanceList;
    List<Double> reverseDistanceList;

    Set<Integer> processedForward;
    Set<Integer> processedReverse;

    public double computeDist(Graph CHGraph, int source, int target) {
        double estimate = Double.MAX_VALUE;
        forwardDistanceList = new ArrayList<>();
        reverseDistanceList = new ArrayList<>();

        for (int i = 0; i < CHGraph.getNodeAmount(); i++) {
            forwardDistanceList.add(Double.MAX_VALUE);
            reverseDistanceList.add(Double.MAX_VALUE);
        }

        forwardDistanceList.set(source, 0.0);
        reverseDistanceList.set(target, 0.0);

        processedForward = new HashSet<>();
        processedReverse = new HashSet<>();

        Comparator<Integer> compForward = Comparator.comparingDouble(forwardDistanceList::get);
        Comparator<Integer> compReverse = Comparator.comparingDouble(reverseDistanceList::get);
        forwardQ = new JavaMinPriorityQueue(compForward, CHGraph.getNodeAmount());
        reverseQ = new JavaMinPriorityQueue(compReverse, CHGraph.getNodeAmount());

        forwardQ.add(source);
        reverseQ.add(target);

        while (!forwardQ.isEmpty() || !reverseQ.isEmpty()) {
            if (!forwardQ.isEmpty()) {
                int nodeForward = forwardQ.poll();
                processedForward.add(nodeForward);
                if (forwardDistanceList.get(nodeForward) <= estimate) {
                    relaxEdgesBi(nodeForward, "f");
                }
                if (processedReverse.contains(nodeForward)) {
                    if (forwardDistanceList.get(nodeForward) + reverseDistanceList.get(nodeForward) < estimate) {
                        estimate = forwardDistanceList.get(nodeForward) + reverseDistanceList.get(nodeForward);
                    }
                }
            }

            if (!reverseQ.isEmpty()) {
                int nodeReverse = reverseQ.poll();
                processedReverse.add(nodeReverse);
                if (distances.get(nodeReverse).revDistance <= estimate) {
                    relaxEdgesBi(nodeReverse, "r");
                }
                if (processedForward.contains(nodeReverse)) {
                    if (reverseDistanceList.get(nodeReverse) + forwardDistanceList.get(nodeReverse) < estimate) {
                        estimate = reverseDistanceList.get(nodeReverse) + forwardDistanceList.get(nodeReverse);
                    }
                }
            }
        }

        if (estimate == Double.MAX_VALUE) {
            return -1;
        }

        return estimate;
    }

    private void relaxEdgesBi(int node, String str) {
        List<Edge> adjList = graph.getAdjList().get(node);
        System.out.println(adjList);
        if (str.equals("f")) {
            for (Edge edge : adjList) {
                int temp = edge.to;
                double cost = edge.d;
                System.out.println(ranks.get(node) + " " + ranks.get(temp));
                if (forwardDistanceList.get(node) + cost < forwardDistanceList.get(temp) && ranks.get(node) < ranks.get(temp)) {
                    forwardDistanceList.set(temp, forwardDistanceList.get(node) + cost);
                    forwardQ.updatePriority(temp);
                }
            }
        } else {
            for (Edge edge : adjList) {
                int temp = edge.to;
                double cost = edge.d;
                System.out.println(ranks.get(node) + " " + ranks.get(temp));
                if (reverseDistanceList.get(node) + cost < reverseDistanceList.get(temp) && ranks.get(node) < ranks.get(temp)) {
                    reverseDistanceList.set(temp, reverseDistanceList.get(node) + cost);
                    reverseQ.updatePriority(temp);
                }
            }
        }
    }


    static class Distance {
        // Ids are made so that we dont have to reinitialize every time the distance value to infinity.

        int contractID;        //id for the vertex that is going to be contracted.
        int sourceID;           //it contains the id of vertex for which we will apply dijkstra while contracting.

        double distance;        //stores the value of distance while contracting.

        //used in query time for bidirectional dijkstra algorithm
        int forwardQueryID;    //for forward search.
        int reverseQueryID;    //for backward search.

        double queryDist;    //for forward distance.
        double revDistance;    //for backward distance.

        public Distance() {
            this.contractID = -1;
            this.sourceID = -1;

            this.forwardQueryID = -1;
            this.reverseQueryID = -1;

            this.distance = Integer.MAX_VALUE;

            this.revDistance = Integer.MAX_VALUE;
            this.queryDist = Integer.MAX_VALUE;
        }
    }
}
