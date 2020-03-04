package model;

import java.util.*;

public class Graph {
    private List<Node> nodeList;
    private List<List<Edge>> adjList;
    private int nodeSize;

    public int getNodeAmount() {
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

    public void addEdge(Node node1, Node node2, double d) {
        adjList.get(node1.index).add(new Edge(node2.index, d));
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public void resetPathTrace() {
        for (List<Edge> edges : adjList) {
            for (Edge edge : edges) {
                edge.visitedReverse = false;
                edge.visited = false;
                edge.inPath = false;
            }
        }
    }

    public List<List<Edge>> reversePaintEdges(List<List<Edge>> revAdjList, List<List<Edge>> mergeList) {
        List<List<Edge>> restoredList = reverseAdjacencyList(revAdjList);
        for (int i = 0; i < restoredList.size(); i++) {
            for (Edge e : restoredList.get(i)) {
                if (e.visited) {
                    for (int j = 0; j < mergeList.get(i).size(); j++) {
                        Edge reciever = mergeList.get(i).get(j);
                        if (e.to == reciever.to) {
                            if (reciever.inPath) {
                                e.inPath = true;
                            }
                            e.visitedReverse = true;
                            mergeList.get(i).set(j, e);
                        }
                    }
                }
            }
        }
        return mergeList;
    }

    public List<List<Edge>> reverseAdjacencyList(List<List<Edge>> originalList) {
        List<List<Edge>> reversedList = new ArrayList<>();
        for (int i = 0; i < nodeSize; i++) {
            reversedList.add(new LinkedList<>());
        }
        for (int i = 0; i < originalList.size(); i++) {
            for (Edge e : originalList.get(i)) {
                Edge replacement = new Edge(i, e.d, e.visited, e.inPath, e.isDrawn, e.visitedReverse);
                reversedList.get(e.to).add(replacement);
            }
        }
        return reversedList;
    }

    public int getNumberOfEdges() {
        int total = 0;
        for (List<Edge> sublist : adjList) {
            if (sublist != null) {
                total += sublist.size();
            }
        }

        return total;
    }
}
