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
    private List<Integer> orderPos;
    private List<Distance> distances;

    public ContractionHierarchies(Graph graph) {
        this.graph = graph;

        importanceQueue = new JavaMinPriorityQueue(getImportanceComparator(), graph.getNodeAmount());

        graphUtil = new GraphUtil(graph);
        inDegreeMap = graphUtil.getInDegreeNodeMap();

        contracted = new ArrayList<>();
        importance = new ArrayList<>();
        contractedNeighbours = new ArrayList<>();
        orderPos = new ArrayList<>();
        distances = new ArrayList<>();

        initializeLists();
        setInitialImportance();
    }

    private void initializeLists() {
        for (Node ignored : graph.getNodeList()) {
            contracted.add(false);
            importance.add(0);
            contractedNeighbours.add(0);
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

    // We eliminate nodes one by one in some order and add 'Shortcuts' to preserve distances.
    public Graph preprocess() {
        // The Graph with augmented edges (added all shortcuts)
        augmentedGraph = new Graph(graph);
        // Contains the vertices in the order they are contracted
        int[] nodeOrdering = new int[graph.getNodeAmount()];
        // Stores the number of vertices that are contracted
        int extractNum = 0;

        while (!importanceQueue.isEmpty()) {
            int n = importanceQueue.poll();
            updateImportance(n);

            // If the vertex's recomputed importance is still minimum then contract it
            // This is called 'Lazy Update'
            if (importanceQueue.size() != 0 && importance.get(n) > importanceQueue.peek()) {
                importanceQueue.add(n);
                continue;
            }

            nodeOrdering[extractNum] = n;
            // orderPos.set(n, extractNum);
            extractNum++;

            // Contraction part
            contractNode(n, extractNum - 1);
        }

        return augmentedGraph;
    }

    private void contractNode(int n, int contractID) {
        // Set contracted == true for the current node.
        contracted.set(n, true);

        // Update the given node's neighbors about that the given node has been contracted.
        updateNeighbours(n);

        for (Node inNode : inDegreeMap.get(n)) {
            int inNodeIndex = inNode.index;
            if (contracted.get(inNodeIndex)) {
                continue;
            }

            // Find the inCost of an edge.
            double inCost = getInCost(n, inNodeIndex);

            // This adds shortcuts if no witness path was found.
            List<Edge> outEdges = graph.getAdjList().get(inNodeIndex);
            for (Edge outEdge : outEdges) {
                int outNode = outEdge.to;
                double outCost = outEdge.d;

                if (contracted.get(outNode)) {
                    continue;
                }

                double totalCost = inCost + outCost;
                // Checks if a witness path exists. If it doesnt we will add a shortcut bypassing n.
                if (!witnessPathExists(inNodeIndex, n, outNode, totalCost, contractID)) {
                    augmentedGraph.addEdge(inNodeIndex, outNode, totalCost);
                    augmentedGraph.addEdge(outNode, inNodeIndex, totalCost);
                }
            }
        }
    }

    private double getInCost(int n, int inNodeIndex) {
        double inCost = 0;
        for (Edge e : graph.getAdjList().get(inNodeIndex)) {
            if (e.to == n) {
                inCost = e.d;
                break;
            }
        }
        return inCost;
    }

    // Checks if there exists a shorter path between inNode and outNode not going through n.
    // This is done by: for each predecessor v_i of n, run Dijkstra from v_i ignoring n.
    private boolean witnessPathExists(int inNode, int node, int outNode, double maxCost, int contractID) {
        Queue<Integer> queue = new PriorityQueue<>(getPriorityComparator());

        distances.get(inNode).distance = 0;
        distances.get(inNode).contractId = contractID;

        queue.clear();
        queue.add(inNode);

        int i = 0;
        while (queue.size() != 0) {
            int next = queue.poll();
            if (next == outNode && distances.get(next).distance < maxCost) {
                return true;
            } else if (i > 3) {
                break;
            }

            for (int j = 0; j < augmentedGraph.getAdjList().get(next).size(); j++) {
                int temp = augmentedGraph.getAdjList().get(next).get(i).to;
                double cost = augmentedGraph.getAdjList().get(next).get(i).d;

                // If the Edge is going to n, we will discard it and continue.
                if(contracted.get(temp) || temp == node) {
                    continue;
                }

                if(checkId(next, temp) || distances.get(temp).distance > distances.get(next).distance + cost){
                    distances.get(temp).distance = distances.get(next).distance + cost;
                    distances.get(temp).contractId = contractID;

                    queue.remove(temp);
                    queue.add(temp);
                }
            }

            i++;
        }

        return false;
    }

    //compare the ids whether id of source to target is same if not then consider the target vertex distance=infinity.
    private boolean checkId(int source, int target){
        return distances.get(source).contractId != distances.get(target).contractId;
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
        int inDegree = inDegreeMap.get(n).size();
        int outDegree = graphUtil.getOutDegree(n);
        int numberOfShortcuts = inDegree * outDegree;
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
     * The idea is that we want to contract important nodes later. The shortcutCover sc(n) is the
     * number of neighbours m of n such that we have shortcut to or from m after contracting n.
     * If the sc(n) is big, many nodes depend on n. We will contract a node with small sc(n).
     */
    private int shortcutCover(int n) {
        return graph.getAdjList().get(n).size();
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
        return edgeDifference(n) + contractedNeighbours(n) + shortcutCover(n) + nodeLevel(n);
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

    private Comparator<Integer> getPriorityComparator() {
        return Comparator.comparingDouble(i -> distances.get(i).distance);
    }

    static class Distance {
        //Ids are made so that we dont have to reinitialize everytime the distance value to infinity.

        int contractId;        //id for the vertex that is going to be contracted.
        int sourceId;           //it contains the id of vertex for which we will apply dijkstra while contracting.

        double distance;        //stores the value of distance while contracting.

        //used in query time for bidirectional dijkstra algo
        int forwardQueryId;    //for forward search.
        int reverseQueryId;    //for backward search.

        double queryDist;    //for forward distance.
        double revDistance;    //for backward distance.

        public Distance() {
            this.contractId = -1;
            this.sourceId = -1;

            this.forwardQueryId = -1;
            this.reverseQueryId = -1;

            this.distance = Integer.MAX_VALUE;

            this.revDistance = Integer.MAX_VALUE;
            this.queryDist = Integer.MAX_VALUE;
        }
    }
}
