package model;

import java.io.Serializable;
import java.util.*;

public class Graph implements Serializable {
    private List<Node> nodeList;
    private List<List<Edge>> adjList;

    private Set<Integer> landmarks;
    private int nodeSize;

    public Graph(int nodeSize) {
        init(nodeSize);
    }

    public void init(int size) {
        this.nodeSize = size;
        nodeList = new ArrayList<>();
        adjList = new ArrayList<>();
        landmarks = new HashSet<>();
        for (int i = 0; i < size; i++) {
            adjList.add(emptyAdjList());
        }
    }

    public int[] BFSMaxDistance(int startNode) {
        int[] hop = new int[nodeSize];
        boolean[] seen = new boolean[nodeSize];
        Queue<Integer> queue = new ArrayDeque<>(nodeSize);
        queue.add(startNode);
        hop[startNode] = 0;
        seen[startNode] = true;
        while (!queue.isEmpty()) {
            int top = queue.poll();
            for (Edge e : adjList.get(top)) {
                if (!seen[e.to]) {
                    hop[e.to] = hop[top] + 1;
                    queue.add(e.to);
                    seen[e.to] = true;
                }
            }
        }

        return hop;
    }

    public Set<Integer> extractLandmarksFarthest(int goalAmount) {
        // Current implementation is 'FarthestB' (B - breadth)
        // Simple but not necessarily best. MaxCover yields better results.
        // TODO: MaxCover for landmark selection
        int[][] resArr = new int[goalAmount][nodeSize];
        if (landmarks.isEmpty()) {
            Random random = new Random();
            random.setSeed(666974757);
            int startNode = random.nextInt(nodeList.size());
            int[] arr = BFSMaxDistance(startNode);
            int max = 0;
            for (int i = 0; i < arr.length; i++) {
                max = arr[i] > arr[max] ? i : max;
            }
            resArr[0] = arr;
            landmarks.add(max);
        }

        while (landmarks.size() < goalAmount) {
            int highestMinimal = 0;
            int furthestCandidate = 0;
            for (Node n : nodeList) {
                if (landmarks.contains(n.index)) continue;
                int lowestCandidateDistance = Integer.MAX_VALUE;
                for (int i = 0; i < landmarks.size(); i++) {
                    if (lowestCandidateDistance > resArr[i][n.index]) {
                        lowestCandidateDistance = resArr[i][n.index];
                    }
                }
                if (lowestCandidateDistance > highestMinimal) {
                    furthestCandidate = n.index;
                    highestMinimal = lowestCandidateDistance;
                }
            }
            resArr[landmarks.size()] = BFSMaxDistance(furthestCandidate);
            landmarks.add(furthestCandidate);
        }
        return landmarks;
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
        for (int i = 0; i < nodeSize; i++) {
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

    public int getNumberOfEdges() {
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
        node.index = nodeSize;
        nodeList.add(node);
        nodeList.indexOf(node);
        adjList.add(emptyAdjList());
        nodeSize++;
    }

    public int getNodeAmount() {
        return nodeSize;
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
        adjList = adjList.subList(0, nodeSize - number);
        for (int i = 0; i < number; i++) {
            nodeSize--;
            nodeList.remove(nodeSize);
            removeAllEdgesTo(nodeSize);
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

    public Set<Integer> getLandmarks() {
        return landmarks;
    }
}
