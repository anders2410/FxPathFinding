package model;

import paths.SSSP;
import paths.ShortestPathResult;
import paths.factory.LandmarksFactory;
import paths.strategy.HeuristicFunction;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Graph implements Serializable {
    private List<Node> nodeList;
    private List<List<Edge>> adjList;

    private Set<Integer> landmarks;
    private Map<Integer, int[]> landmarksDistancesBFS;
    private int nodeSize;

    public Graph(int nodeSize) {
        init(nodeSize);
    }

    public void init(int size) {
        this.nodeSize = size;
        nodeList = new ArrayList<>();
        adjList = new ArrayList<>();
        landmarks = new LinkedHashSet<>();
        landmarksDistancesBFS = new HashMap<>();
        for (int i = 0; i < size; i++) {
            adjList.add(emptyAdjList());
        }
    }

    public int[] BFSMaxDistance(int startNode) {
        int[] hop = new int[nodeSize];
        Arrays.fill(hop, Integer.MAX_VALUE);
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

    public void maxCoverLandmarks(int goalAmount) {
        landmarksAvoid(goalAmount);
        Set<Integer> candidateSet = new HashSet<>(landmarks);
        int avoidCall = 1;
        while (candidateSet.size() != 4 * goalAmount || avoidCall != goalAmount * 5) {
            removeRandomCandidateAndGraphMarks(candidateSet);
            landmarksAvoid(goalAmount);
            candidateSet.addAll(landmarks);
        }

    }

    private int calculateCoverCost(Set<Integer> potentialLandmarks) {
        return 0;
    }

    private void removeRandomCandidateAndGraphMarks(Set<Integer> candidateSet) {
        Iterator<Integer> iterator = landmarks.iterator();
        while (iterator.hasNext()) {
            int temp = (Math.random() <= 0.5) ? 1 : 2;
            if (temp == 1) {
                int lmark = iterator.next();
                candidateSet.remove(lmark);
                iterator.remove();
            }
        }
    }

    public void landmarksAvoid(int goalAmount) {
        if (landmarks == null || landmarks.isEmpty()) {
            Random random = new Random();
            random.setSeed(664757);
            int randomInitialLandmark = random.nextInt(nodeList.size());
            landmarks.add(randomInitialLandmark);
            landmarksDistancesBFS.put(randomInitialLandmark, BFSMaxDistance(randomInitialLandmark));
            avoidGetLeaf();
            landmarks.remove(randomInitialLandmark);
        }

        while (landmarks.size() < goalAmount) {
            avoidGetLeaf();
        }
    }

    private void avoidGetLeaf() {
        // TODO: 16-03-2020 Avoid trapping in one-way streets, possibly by maxCover or just hack our way out of it
        int rootNode = getFurthestCandidateLandmark();
        ShortestPathResult SPT = SSSP.singleToAllPath(rootNode);
        double[] weightedNodes = new double[nodeSize];
        SSSP.applyFactory(new LandmarksFactory());
        HeuristicFunction S = SSSP.getHeuristicFunction();
        for (int i = 0; i < SPT.nodeDistance.size(); i++) {
            double heuristicVal = S.apply(rootNode, i);
            double val = SPT.nodeDistance.get(i) - heuristicVal;
            weightedNodes[i] = val;
        }
        Map<Integer, List<Integer>> pathInversed = SPT.pathMap.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        double maxWeight = -1.0;
        int bestCandidate = 0;
        double[] sizeNodes = new double[nodeSize];
        for (int i = 0; i < nodeSize; i++) {
            if (i == rootNode) {
                continue;
            }
            double weighSumNode = findSumInTree(i, pathInversed, weightedNodes);
            sizeNodes[i] = weighSumNode;
            if (maxWeight <= weighSumNode) {
                bestCandidate = i;
                maxWeight = weighSumNode;
            }
        }
        int leaf = findLeaf(bestCandidate, pathInversed, sizeNodes);
        landmarks.add(leaf);
    }

    private Integer getLatestLandMark() {
        if (landmarks.isEmpty()) {
            return null;
        }
        Iterator<Integer> iterator = landmarks.iterator();
        int lastElem = -1;
        while (iterator.hasNext()) {
            lastElem = iterator.next();
        }
        return lastElem;
    }

    private int findLeaf(int bestCandidate, Map<Integer, List<Integer>> pathInversed, double[] sizedNodes) {
        if (pathInversed.get(bestCandidate) == null) {
            return bestCandidate;
        }
        double maxSizeSoFar = -Double.MAX_VALUE;
        for (Integer i : pathInversed.get(bestCandidate)) {
            if (sizedNodes[i] > maxSizeSoFar) {
                maxSizeSoFar = sizedNodes[i];
                bestCandidate = i;
            }
        }
        return findLeaf(bestCandidate, pathInversed, sizedNodes);
    }

    private double findSumInTree(int treeRoot, Map<Integer, List<Integer>> pathInversed, double[] weightedNodes) {
        if (landmarks.contains(treeRoot)) {
            return -1.0;
        }
        if (pathInversed.get(treeRoot) == null) {
            return 0;
        }
        double sum = weightedNodes[treeRoot];
        for (Integer i : pathInversed.get(treeRoot)) {
            double subTreeSum = findSumInTree(i, pathInversed, weightedNodes);
            if (subTreeSum == -1.0) return -1.0;
            sum += subTreeSum;
        }
        return sum;
    }

    public void extractLandmarksFarthest(int goalAmount) {
        // Current implementation is 'FarthestB' (B - breadth)
        // Simple but not necessarily best. MaxCover yields better results.
        // TODO: MaxCover for landmark selection
        if (landmarks.isEmpty()) {
            Random random = new Random();
            random.setSeed(666974757);
            int startNode = random.nextInt(nodeList.size());
            int[] arr = BFSMaxDistance(startNode);
            int max = 0;
            for (int i = 0; i < arr.length; i++) {
                max = arr[i] > arr[max] ? i : max;
            }
            landmarksDistancesBFS.put(startNode, arr);
            landmarks.add(max);
        }

        while (landmarks.size() < goalAmount) {
            int furthestCandidate = getFurthestCandidateLandmark();
            landmarksDistancesBFS.put(furthestCandidate, BFSMaxDistance(furthestCandidate));
            landmarks.add(furthestCandidate);
        }
    }

    private int getFurthestCandidateLandmark() {
        int highestMinimal = 0;
        int furthestCandidate = 0;
        for (Node n : nodeList) {
            if (landmarks.contains(n.index)) continue;
            int lowestCandidateDistance = Integer.MAX_VALUE;
            for (Integer i : landmarksDistancesBFS.keySet()) {
                int stepDistance = landmarksDistancesBFS.get(i)[n.index];
                if (lowestCandidateDistance > stepDistance) {
                    lowestCandidateDistance = stepDistance;
                }
            }
            if (lowestCandidateDistance > highestMinimal && lowestCandidateDistance != Integer.MAX_VALUE) {
                furthestCandidate = n.index;
                highestMinimal = lowestCandidateDistance;
            }
        }
        return furthestCandidate;
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
