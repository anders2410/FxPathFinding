package paths;

import model.Graph;

import java.util.List;

public class ContractionHiearchiesWorking {
    // The road network graph.
    // NOTE: the contraction hierarchies algorithm *modifies* this graph by adding
    // shortcuts, so this must be a *non-const* reference or pointer, and you should
    // be aware that by using this class, the original graph will be modified.
    private Graph graph;

    // Object for the various execution of  Dijkstra's algorithm on the augmented
    // graph.
    private DijkstrasAlgorithm dijkstra;

    // The ordering of the nodes. This is simply a permutation of {0, ..., n-1},
    // where n is the number of nodes; nodeOrdering[i] simply contains the index
    // of the i-th node in the ordering, for i = 0, ..., n-1.
    private List<Integer> nodeOrdering;

    // NEW(lecture-7): This is the "reverse" of the array above. If the i-th node
    // contracted is u, then orderOfNode[u] = i.
    private List<Integer> orderOfNode;

    private List<Boolean> contracted;
    private List<Integer> importance;
    private List<Integer> contractedNeighbours;

    ContractionHiearchiesWorking(Graph graph) {
        this.graph = graph;
    }

    // Central contraction routine: contract the i-th node in the ordering,
    // ignoring nodes 1, ..., i - 1 in the ordering and their adjacent arcs.
    // IMPLEMENTATION NOTE: To ignore nodes (and their adjacent arcs), you can
    // simply use the arcFlag member of the Arc class. Initially, set all arcFlags
    // to 1, and as you go along and contract nodes, simply set the flags of the
    // arcs adjacent to contracted nodes to 0. And make sure to call
    // setConsiderArcFlags(true) on the dijkstra  object below.
    //
    // NEW(lecture-7): additional argument that says whether we really want to
    // contract the node or just compute the edge difference. Default is false. If
    // true, don't change anything in the graph (don't add any arcs and don't
    // set any arc flags to false) and return the edge difference.
    private int contractNode(int i) {
        return 0;
    }

    // NEW(lecture-7): Do the precomputation by contracting all nodes in the order
    // of their edge differences, and adding shortcuts on the way.
    // IMPLEMENTATION NOTE: Maintain nodes in a priority queue with key = edge
    // difference. When contracting the i-th node, and it is node u, set
    // orderOfNode[u] = i; see below. After all nodes have been contracted (and
    // various shortcuts added to the original graph), reset the arc flags such
    // that only the arc flags for arcs u, v with orderOfNode[u] < orderOfNode[v]
    // are true.
    public void precompute() {

    }

    // Compute the shortest paths from the given source to the given target node.
    // Returns the cost of the shortest path (no need to compute the path).
    // IMPLEMENTATION NOTE: You need two Dijkstra per query, both considering only
    // the arc flags set at the end of the precomputation above. (No need to
    // change any arc flags at query time.) Make sure that you avoid resetting
    // *all* dist values for each Dijsktra. Instead reset only those that were
    // actually changed in the last Dijkstra.
    public double computeShortestPath(int sourceNodeId, int targetNodeId) {
        return 0.0;
    }

    private static class DijkstrasAlgorithm {
        private DijkstrasAlgorithm() {

        }
    }
}
