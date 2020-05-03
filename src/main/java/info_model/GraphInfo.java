package info_model;

import model.Edge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GraphInfo implements Serializable {
    private List<NodeInfo> nodeList;
    private List<List<EdgeInfo>> adjList;
    private int nodeAmount;

    public GraphInfo(int n) {
        init(n);
    }

    public void init(int size) {
        this.nodeAmount = size;
        nodeList = new ArrayList<>();
        adjList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            adjList.add(new ArrayList<>());
        }
    }

    public void addEdge(EdgeInfo edgeInfo) {

    }

    public void setNodeList(List<NodeInfo> nodeList) {
        this.nodeList = nodeList;
    }

    public List<NodeInfo> getNodeList() {
        return nodeList;
    }

    public List<List<EdgeInfo>> getAdjList() {
        return adjList;
    }

    public EdgeInfo getEdge(Edge edge) {
        for (EdgeInfo edgeInfo : adjList.get(edge.from)) {
            if (edge.to == edgeInfo.getTo()) {
                return edgeInfo;
            }
        }
        return null;
    }
}
