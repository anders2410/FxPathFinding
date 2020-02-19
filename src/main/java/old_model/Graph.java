package old_model;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Graph {
    private List<Node> nodeList;
    private List<List<Edge>> adjList;
    private int nodeSize;

    public int getNodeSize() {
        return nodeSize;
    }

    public Graph(int nodeSize) {
        init(nodeSize);
    }

    public void init(int size) {
        this.nodeSize = size;
        nodeList = new ArrayList<>();
        adjList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            adjList.add(new LinkedList<>());
        }
    }

    public List<List<Edge>> getAdjList() {
        return adjList;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public void addEdge(Node node1, Node node2, float d) {
        adjList.get(node1.index).add(new Edge(node2.index, d));
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public void resetPathTrace() {
        for (List<Edge> edges : adjList) {
            for (Edge edge : edges) {
                edge.visited = false;
                edge.inPath = false;
            }
        }
    }
}
