package paths;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;

public class ReachProcessor {
    private Graph graph;

    double reachMetric(int node) {
        return 0.0;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public void computeReachBound(Graph g, int[] boundCeilingArr) {
        double[] bounds = new double[g.getNodeAmount()];
        Arrays.fill(bounds, Double.MAX_VALUE);
        Graph subGraph = g;
        for (int i = 0; i < 100; i++) {
            subGraph = computeReachBoundsSubgraph(g, subGraph, i, bounds);
        }
    }

    private Graph computeReachBoundsSubgraph(Graph g, Graph subGraph, int b, double[] bounds) {
        double maxReachOriginalGraph;
        List<Node> originalNodeList = g.getNodeList();
        List<Node> subGraphNodeList = subGraph.getNodeList();
        //reachSPT is reach of nodes in least-cost path trees. (SPT != least-cost, but close). Least-cost uses reach metric, SPT uses weight metric.
        double[] reachLCPT = new double[bounds.length];
        maxReachOriginalGraph = exclusiveOriginalGraphReachBound(g, subGraph, bounds, originalNodeList, subGraphNodeList);
        for (int i = 0; i < subGraphNodeList.size(); i++) {
            if (subGraphNodeList.get(i) != null) {
                bounds[i] = 0;
                reachLCPT[i] = 0;
            }
        }
        Graph connectiveGraph = createConnectiveGraph(g, subGraph);
        Map<Integer, Double> maxEstimateBoundIncreaseMap = new HashMap<>();
        Map<Integer, Double> maxReachMetricMap = new HashMap<>();


        return null;
    }

    private Graph createConnectiveGraph(Graph g, Graph subGraph) {
        Graph connectiveGraph = new Graph(g.getNodeAmount());
        connectiveGraph.setNodeList(subGraph.getNodeList());
        for (int i = 0; i < subGraph.getAdjList().size(); i++) {
            for (Edge e : subGraph.getAdjList().get(i)) {
                if (subGraph.getNodeList().get(e.to) == null) {
                    connectiveGraph.getAdjList().get(i).add(e);
                    connectiveGraph.getNodeList().set(i, g.getNodeList().get(e.to));
                }
            }

        }
        return connectiveGraph;
    }

    private double exclusiveOriginalGraphReachBound(Graph g, Graph subGraph, double[] bounds, List<Node> originalNodeList, List<Node> subGraphNodeList) {
        double maxReachOriginalGraph = 0;
        if (!g.getNodeList().equals(subGraph.getNodeList())) {
            double maxSoFar = -1;
            for (int i = 0; i < originalNodeList.size(); i++) {
                if (originalNodeList.get(i) != subGraphNodeList.get(i)) {
                    maxSoFar = Math.max(bounds[i], maxSoFar);
                }
            }
            maxReachOriginalGraph = maxSoFar;
        }
        return maxReachOriginalGraph;
    }

}
