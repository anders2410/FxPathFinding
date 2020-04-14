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

    private List<Boolean> contracted;
    private List<Integer> importance;
    private List<Integer> contractedNeighbours;

    public ContractionHierarchies(Graph graph) {
        this.graph = graph;

        importanceQueue = new JavaMinPriorityQueue(getImportanceComparator(), graph.getNodeAmount());

        graphUtil = new GraphUtil(graph);
        inDegreeMap = graphUtil.getInDegreeNodeMap();

        contracted = new ArrayList<>();
        importance = new ArrayList<>();
        contractedNeighbours = new ArrayList<>();
        initializeLists();
    }

    private void initializeLists() {
        for (Node ignored : graph.getNodeList()) {
            contracted.add(false);
            importance.add(0);
            contractedNeighbours.add(0);
        }
    }

    /**
     * When contracting a node n, for any pair of edges (u,n) and (n,w), we want to check whether
     * there is a witness path from u to w bypassing n with length at most l(u,n) + l(n,w). Then there
     * is no need to add a shortcut from u to w.
     */
    public boolean witnessPathExists(Node n) {
        return false;
    }

    // Setting the initial importance of all the nodes
    private void setInitialImportance() {
        List<Node> nodeList = graph.getNodeList();
        for (Node n : nodeList) {
            importance.set(n.index, calculateImportance(n));
            importanceQueue.add(n.index);
        }
    }

    // Update the importance of a node
    private void updateImportance(Node n) {
        importance.set(n.index, calculateImportance(n));
    }

    /**
     * The edgeDifference is defined by edgeDifference = s(n) - in(n) - out(n). Where s(n) is the number of
     * added shortcuts, in(n) is in degree and out(n) is out degree. We want to contract nodes
     * with a small edgeDifference.
     */
    private int edgeDifference(Node n) {
        int inDegree = inDegreeMap.get(n.index).size();
        int outDegree = graphUtil.getOutDegree(n);
        int numberOfShortcuts = inDegree * outDegree;
        return numberOfShortcuts - inDegree - outDegree;
    }

    /**
     * We are interested in spreading out the contracted nodes across the graph so they
     * do not cluster together. We will contract nodes with a small number of already contracted
     * neighbours.
     */
    private int contractedNeighbours(Node n) {
        return contractedNeighbours.get(n.index);
    }

    /**
     * The idea is that we want to contract important nodes later. The shortcutCover sc(n) is the
     * number of neighbours m of n such that we have shortcut to or from m after contracting n.
     * If the sc(n) is big, many nodes depend on n. We will contract a node with small sc(n).
     */
    private int shortcutCover(Node n) {
        return graph.getAdjList().get(n.index).size();
    }

    /**
     * The Node Level L(n) is an upper bound on the number of edges in the shortest path from any
     * s to n in the augmented path. Initially, L(n) is 0. After contracting node n, for neighbours u
     * of n do L(u) <- max(L(u), L(n) + 1). We contract a node with small L(n).
     */
    private int nodeLevel(Node n) {
        return 0;
    }

    /**
     * The importance is used when constructing the augmented graph. We want to contract all the
     * nodes with low importance first, and then gradually work our way through the graph using
     * a priorityQueue based on the importance.
     *
     * We can optionally experiment with some different weight to all the functions.
     */
    private int calculateImportance(Node n) {
        return edgeDifference(n) + contractedNeighbours(n) + shortcutCover(n) + nodeLevel(n);
    }

    // Update the neighbours of the contracted node that this node has been contracted.
    private void updateNeighbours(Node n) {
        List<Edge> adj = graph.getAdjList().get(n.index);
        for(Edge edge : adj) {
            contractedNeighbours.set(edge.to, contractedNeighbours.get(edge.to + 1));
        }

        List<Node> inDegreeList = inDegreeMap.get(n.index);
        for(Node node : inDegreeList) {
            contractedNeighbours.set(node.index, contractedNeighbours.get(node.index + 1));
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
}
