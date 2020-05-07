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
        adjList.get(edgeInfo.getFrom()).add(edgeInfo);
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
        return new EdgeInfo(0,0, -1, Surface.UNKNOWN);
    }

    public void printAdjList() {
        System.out.println("[AdjList] size " + nodeAmount);
        for (int i = 0; i < adjList.size(); i++) {
            List<EdgeInfo> edges = adjList.get(i);
            System.out.print("From " + i + " to: ");
            for (EdgeInfo edge : edges) {
                System.out.print(edge.getTo());
                System.out.print(" maxSpeed=");
                System.out.print(edge.getMaxSpeed());
                System.out.print("; ");
            }
            System.out.println();
        }
    }

    public int getNodeAmount() {
        return nodeAmount;
    }
}
