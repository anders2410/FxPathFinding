package paths.preprocessing;

import datastructures.DuplicatePriorityQueueNode;
import datastructures.JavaDuplicateMinPriorityQueue;
import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;
import java.util.function.BiConsumer;

// This is gonna be a fest!
public class ArcFlags {
    private final Graph graph;
    private final List<List<Edge>> revGraphAdjList;

    double[] nodeDist;
    HashSet<?>[] flags;
    private Set<Integer> dijkstraVisited;

    private BiConsumer<Long, Long> progressListener = (l1, l2) -> { };

    public ArcFlags(Graph graph) {
        this.graph = graph;
        revGraphAdjList = graph.getReverse(graph.getAdjList());
        nodeDist = new double[graph.getNodeAmount()];
        flags = new HashSet<?>[graph.getNodeAmount()];
    }

    public void preprocess(int k) {
        initializeFlags();
    }

    private void initializeFlags() {
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            Node n = graph.getNodeList().get(i);
            Map<Integer, Integer> backwardsPathTree = dijkstra(n.index, revGraphAdjList);
            Set<Map.Entry<Integer, Integer>> shortestPathTree = backwardsPathTree.entrySet();
            HashMap<Integer, List<Integer>> leastCostTreeH = new HashMap<>();
            for (Map.Entry<Integer, Integer> entryPair : shortestPathTree) {
                List<Integer> list = leastCostTreeH.computeIfAbsent(entryPair.getValue(), abe -> new ArrayList<>());
                list.add(entryPair.getKey());
                leastCostTreeH.replace(entryPair.getValue(), list);
            }
            flags[i] = getEdgeSet(leastCostTreeH, n.index, new HashSet<>());
        }
    }

    // Dijkstra running on the reverse graph
    private Map<Integer, Integer> dijkstra(int source, List<List<Edge>> adjList) {
        dijkstraVisited = new HashSet<>();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            nodeDist[i] = Double.MAX_VALUE;
        }

        nodeDist[source] = 0.0;
        Map<Integer, Integer> pathMap = new HashMap<>();
        JavaDuplicateMinPriorityQueue priorityQueue = new JavaDuplicateMinPriorityQueue();
        DuplicatePriorityQueueNode queueNode = new DuplicatePriorityQueueNode(source, 0.0);

        priorityQueue.add(queueNode);

        while (!priorityQueue.isEmpty()) {
            boolean newEntryFound = false;
            DuplicatePriorityQueueNode from = null;
            while (!newEntryFound) {
                from = priorityQueue.poll();
                if (from == null) return pathMap;
                if (!dijkstraVisited.contains(from.getIndex())) newEntryFound = true;
            }

            dijkstraVisited.add(from.getIndex());
            for (Edge edge : adjList.get(from.getIndex())) {
                if (nodeDist[from.getIndex()] + edge.d < nodeDist[edge.to]) {
                    nodeDist[edge.to] = nodeDist[from.getIndex()] + edge.d;
                    DuplicatePriorityQueueNode bacon = new DuplicatePriorityQueueNode(edge.to, nodeDist[edge.to]);
                    priorityQueue.insert(bacon);
                    pathMap.put(edge.to, edge.from);
                }
            }
        }

        return pathMap;
    }

    private Edge getEdge(int i, List<Edge> eList) {
        Edge e = null;
        for (Edge edge : eList) {
            if (edge.to == i) {
                e = edge;
                break;
            }
        }
        return e;
    }

    private HashSet getEdgeSet(Map<Integer, List<Integer>> map, int root, HashSet edgeSet) {
        List<Integer> rootList = map.get(root);
        if (rootList == null) return edgeSet;
        for (int i = 0; i < rootList.size(); i++) {
            Integer element = rootList.get(i);
            edgeSet.add(getEdge(element, graph.getAdjList().get(root)));
            getEdgeSet(map, element, edgeSet);
        }
        return edgeSet;
    }
}
