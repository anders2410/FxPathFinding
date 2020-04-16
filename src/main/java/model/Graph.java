package model;

import java.io.Serializable;
import java.util.*;

public class Graph implements Serializable {
    // We should define our own ID as different machines could generate different ID's
    private static final long serialVersionUID = 6529685098267757690L;

    private List<Node> nodeList;
    private List<List<Edge>> adjList;
    private int nodeAmount;

    public Graph(int nodeAmount) {
        init(nodeAmount);
    }

    public Graph(Graph g) {
        this.nodeList = new ArrayList<>(g.getNodeList());
        this.adjList = new ArrayList<>(g.getAdjList());
        for (int i = 0; i < adjList.size(); i++) {
            adjList.set(i, new LinkedList<>(adjList.get(i)));
        }
        this.nodeAmount = g.getNodeAmount();
    }

    public void init(int size) {
        this.nodeAmount = size;
        nodeList = new ArrayList<>();
        adjList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            adjList.add(emptyAdjList());
        }
    }

    private List<Edge> emptyAdjList() {
        return new LinkedList<>();
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

    public void reversePaintEdges(List<List<Edge>> revAdjList, List<List<Edge>> mergeList) {
        List<List<Edge>> restoredList = getReverse(revAdjList);
        for (int i = 0; i < restoredList.size(); i++) {
            for (Edge e : restoredList.get(i)) {
                if (e.visited) {
                    for (int j = 0; j < mergeList.get(i).size(); j++) {
                        Edge receiver = mergeList.get(i).get(j);
                        if (e.to == receiver.to && e.d == receiver.d) {
                            if (receiver.inPath) {
                                e.inPath = true;
                            }
                            e.visitedReverse = true;

                            mergeList.get(i).set(j, e);
                        }
                    }
                }
            }
        }
    }

    public List<List<Edge>> getReverse(List<List<Edge>> originalList) {
        List<List<Edge>> reversedList = new ArrayList<>(originalList.size());
        for (int i = 0; i < nodeAmount; i++) {
            reversedList.add(emptyAdjList());
        }
        for (int i = 0; i < originalList.size(); i++) {
            for (Edge e : originalList.get(i)) {
                Edge replacement = new Edge(i, e.d, e.visited, e.inPath, e.isDrawn, e.visitedReverse);
                reversedList.get(e.to).add(replacement);
            }
        }
        return reversedList;
    }

    public int getEdgeAmount() {
        int total = 0;
        for (List<Edge> sublist : adjList) {
            if (sublist != null) {
                total += sublist.size();
            }
        }

        return total;
    }

    public List<List<Edge>> getAdjList() {
        return adjList;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public void addNode(Node node) {
        node.index = nodeAmount;
        nodeList.add(node);
        nodeList.indexOf(node);
        adjList.add(emptyAdjList());
        nodeAmount++;
    }

    public int getNodeAmount() {
        return nodeAmount;
    }

    public void addEdge(Node from, Node to, double d) {
        adjList.get(from.index).add(new Edge(to.index, d));
    }

    public void addEdge(int from, int to, double d) {
        adjList.get(from).add(new Edge(to, d));
    }

    public void addEdge(Node from, Edge edge) {
        adjList.get(from.index).add(edge);
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
        nodeAmount = nodeList.size();
    }

    public void removeNodesFromEnd(int number) {
        adjList = adjList.subList(0, nodeAmount - number);
        for (int i = 0; i < number; i++) {
            nodeAmount--;
            nodeList.remove(nodeAmount);
            removeAllEdgesTo(nodeAmount);
        }
    }

    private void removeAllEdgesTo(int index) {
        for (List<Edge> edges : adjList) {
            edges.removeIf(edge -> edge.to == index);
        }
    }

    public void setAdjList(List<List<Edge>> adjList) {
        this.adjList = adjList;
    }

    public void printAdjList() {
        System.out.println("[AdjList] size " + nodeAmount);
        for (int i = 0; i < adjList.size(); i++) {
            List<Edge> edges = adjList.get(i);
            System.out.print("From " + i + " to: ");
            for (Edge edge : edges) {
                System.out.print(edge.to);
                System.out.print(" d=");
                System.out.print(edge.d);
                System.out.print("; ");
            }
            System.out.println();
        }
    }
}
