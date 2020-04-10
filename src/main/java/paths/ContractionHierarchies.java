package paths;

import model.Graph;
import model.Node;

import java.util.PriorityQueue;

/**
 * This class implements the algorithm behind Contraction Hierarchies. Both doing the augmented graph
 * and the Bi-directional Djikstras search on it.
 */
public class ContractionHierarchies {
    private Graph graph;
    private Graph augmentedGraph;
    private PriorityQueue<Node> queue;

    public ContractionHierarchies(Graph graph) {
        this.graph = graph;
        this.queue = new PriorityQueue<>();
    }

    /**
     * The edgeDifference is defined by edgeDifference = s(n) - in(n) - out(n). Where s(n) is the number of
     * added shortcuts, in(n) is incoming degree and out(n) is outgoing degree. We want to contract nodes
     * with a small edgeDifference.
     */
    private int edgeDifference(Node n) {
        return 0;
    }

    /**
     * We are interested in spreading out the contracted nodes across the graoh so they
     * do not cluster together. We will contract nodes with a small number of already contracted
     * neighbours.
     */
    private int contractedNeighbours(Node n) {
        return 0;
    }

    /**
     * The idea is that we want to contract important nodes later. The shortcutCover sc(n) is the
     * number og neighbours m of n such that we have shortcut to or from m after contracting n.
     * If the sc(n) is big, many nodes depend on n. We will contract a node with small sc(n).
     */
    private int shortcutCover(Node n) {
        return 0;
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
     */
    private int getImportance(Node n) {
        return edgeDifference(n) + contractedNeighbours(n) + shortcutCover(n) + nodeLevel(n);
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }
}
