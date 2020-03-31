package model;

import paths.GraphUtil;
import paths.SSSP;
import paths.ShortestPathResult;
import paths.factory.LandmarksFactory;
import paths.strategy.HeuristicFunction;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static paths.SSSP.singleToAllPath;

public class Graph implements Serializable {
    private List<Node> nodeList;
    private List<List<Edge>> adjList;

    private int nodeAmount;

    public Graph(int nodeAmount) {
        init(nodeAmount);
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
        List<List<Edge>> restoredList = reverseAdjacencyList(revAdjList);
        for (int i = 0; i < restoredList.size(); i++) {
            for (Edge e : restoredList.get(i)) {
                if (e.visited) {
                    for (int j = 0; j < mergeList.get(i).size(); j++) {
                        Edge receiver = mergeList.get(i).get(j);
                        if (e.to == receiver.to) {
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

    public List<List<Edge>> reverseAdjacencyList(List<List<Edge>> originalList) {
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

    public void addEdge(Node from, Edge edge) {
        adjList.get(from.index).add(edge);
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
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
}
