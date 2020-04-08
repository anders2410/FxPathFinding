package paths;

import model.Graph;

/**
 * This class implements the algorithm behind Contraction Hierarchies. Both doing the augmented graph
 * and the Bi-directional Djikstras search on it.
 */
public class ContractionHierarchies {
    private Graph graph;

    /**
     * The edgeDifference is defined by edgeDifference = s(v) - in(v) - out(v). Where s(v) is the number of
     * added shortcuts, in(v) is incoming degree and out(v) is outgoing degree.
     *
     * @return the edgeDifferece
     */
    private int edgeDifference() {
        return 0;
    }

    private int contractedNeighbours() {
        return 0;
    }

    private int shortcutCover() {
        return 0;
    }

    private int nodeLevel() {
        return 0;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }
}
