package info_model;

import model.Graph;

public class GraphPair {
    private Graph graph;
    private GraphInfo graphInfo;

    public GraphPair(Graph graph, GraphInfo graphInfo) {
        this.graph = graph;
        this.graphInfo = graphInfo;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public GraphInfo getGraphInfo() {
        return graphInfo;
    }

    public void setGraphInfo(GraphInfo graphInfo) {
        this.graphInfo = graphInfo;
    }
}
