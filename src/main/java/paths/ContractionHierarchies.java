package paths;

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
    private Queue<Node> queue;

    private Comparator<Node> comp = new ImportanceComparator();
    private PriorityQueue<Node> importanceQueue = new PriorityQueue<>(comp);
    private Map<Integer, Integer> inDegreeMap;

    private GraphUtil graphUtil;

    public ContractionHierarchies(Graph graph) {
        this.graph = graph;
        this.queue = new PriorityQueue<>();
        graphUtil = new GraphUtil(graph);
        inDegreeMap = graphUtil.getInDegreeMap();
    }

    /**
     * When contracting a node n, for any pair of edges (u,n) and (n,w), we want to check whether
     * there is a witness path from u to w bypassing n with length at most l(u,n) + l(n,w). Then there
     * is no need to add a shortcut from u to w.
     */
    public boolean witnessPathExists(Node n) {
        return false;
    }

    private void calculateInitialImportance() {
        List<Node> nodeList = graph.getNodeList();
        for (Node n : nodeList) {
            n.setImportance(calculateImportance(n));
        }
    }

    private void updateImportance(Node n) {
        n.setImportance(calculateImportance(n));
    }

    /**
     * The edgeDifference is defined by edgeDifference = s(n) - in(n) - out(n). Where s(n) is the number of
     * added shortcuts, in(n) is in degree and out(n) is out degree. We want to contract nodes
     * with a small edgeDifference.
     */
    private int edgeDifference(Node n) {
        int inDegree = inDegreeMap.get(n.index);
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
        return n.getContractedNeighbours();
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
            int temp = edge.to;
            graph.getNodeList().get(temp).setContractedNeighbours(n.getContractedNeighbours() + 1);
        }
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    public static class ImportanceComparator implements Comparator<Node> {
        public int compare(Node n1, Node n2) {
            return Integer.compare(n1.getImportance(), n2.getImportance());
        }
    }
}
