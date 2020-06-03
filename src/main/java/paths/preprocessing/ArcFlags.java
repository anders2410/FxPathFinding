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
    int[][] setDiff;
    HashSet<?>[] flags;
    private Set<Integer> dijkstraVisited;

    private BiConsumer<Long, Long> progressListener = (l1, l2) -> {
    };

    public ArcFlags(Graph graph) {
        this.graph = graph;
        revGraphAdjList = graph.getReverse(graph.getAdjList());
        nodeDist = new double[graph.getNodeAmount()];
        flags = new HashSet<?>[graph.getNodeAmount()];
        setDiff = new int[graph.getNodeAmount()][graph.getNodeAmount()];

    }

    public void preprocess(int k) {
        initializeFlags();
        initialiseCellDifferences();

        List<Set<Integer>> partition = new ArrayList<>();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            partition.add(new HashSet<>(i));
        }
        while (partition.size() > k) {
            double minDiff = Double.MAX_VALUE;
            int i2;
            int i1 = i2 = -1;
            Set<Integer> mergedMinPartition;
            for (int i = 0; i < partition.size(); i++) {
                for (int j = 0; j < partition.size(); j++) {
                    if (setDiff[i][j] < minDiff) {
                        minDiff = setDiff[i][j];
                        i1 = i;
                        i2 = j;
                    }
                }
            }
            mergedMinPartition = new HashSet<>(partition.get(i1));
            boolean mergeWorked = mergedMinPartition.addAll(partition.get(i2));
            if (!mergeWorked) System.out.println("Merged Fucked Up: i1 = " + i1 + ", i2 = " + i2);
            HashSet<Edge> mergedMinEdges = new HashSet<Edge>((Collection<? extends Edge>) flags[i1]);
            mergedMinEdges.addAll((Collection<? extends Edge>) flags[i2]);
            Set<Integer> i2Set = partition.get(i2);
            partition.remove(i1);
            partition.remove(i2Set);
            partition.add(mergedMinPartition);

            for (int i = 0; i < partition.size(); i++) {
                if (partition.get(i).equals(mergedMinPartition)) continue;
                double diffPartitionMinPartition = 0;
                for (int j = 0; j < graph.getNodeAmount(); j++) {
                    List<List<Edge>> adjList = graph.getAdjList();
                    for (int l = 0; l < adjList.get(j).size(); l++) {
                        boolean exclusiveToPartition;
                    }
                }
            }
        }
    }

    private void initialiseCellDifferences() {
        // for all u \prec v \in V
        // Interpreted as: more minimal based on index number
        for (int v = 0; v < graph.getNodeAmount(); v++) {
            for (int u = 0; u < v; u++) {
                setDiff[v][u] = 0;
                setDiff[u][v] = 0;
                for (int l = 0; l < graph.getNodeAmount(); l++) {
                    List<Edge> edges = graph.getAdjList().get(l);
                    for (int m = 0; m < edges.size(); m++) {
                        boolean exclusiveEdge = flags[u].contains(edges.get(m)) && !flags[v].contains(edges.get(m));
                        boolean inverseExclusive = !flags[u].contains(edges.get(m)) && flags[v].contains(edges.get(m));
                        boolean differenceFound = exclusiveEdge || inverseExclusive;
                        if (differenceFound) {
                            setDiff[v][u] = setDiff[v][u] + 1;
                            setDiff[u][v] = setDiff[u][v] + 1;
                        }
                    }
                }
            }
        }
    }

    private void initializeFlags() {
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            Map<Integer, Integer> backwardsPathTree = dijkstra(i, revGraphAdjList);
            Set<Map.Entry<Integer, Integer>> shortestPathTree = backwardsPathTree.entrySet();
            HashMap<Integer, List<Integer>> leastCostTreeH = new HashMap<>();
            for (Map.Entry<Integer, Integer> entryPair : shortestPathTree) {
                List<Integer> list = leastCostTreeH.computeIfAbsent(entryPair.getValue(), abe -> new ArrayList<>());
                list.add(entryPair.getKey());
                leastCostTreeH.replace(entryPair.getValue(), list);
            }
            flags[i] = getEdgeSet(leastCostTreeH, i, new HashSet<>());
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
