package paths.preprocessing;

import model.Edge;
import model.Graph;
import model.Node;
import model.ModelUtil;
import paths.SSSP;
import paths.ShortestPathResult;
import paths.factory.LandmarksFactory;
import paths.strategy.HeuristicFunction;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Landmarks {
    private Set<Integer> landmarkSet;
    private Map<Integer, int[]> landmarksDistancesBFS;
    private Graph graph;
    private BiConsumer<Double, Double> progressListener;
    private double progressedIndicator;
    private HashMap<Integer, List<Double>> landmarksDistancesActual;

    public Landmarks(Graph graph) {
        this.graph = graph;
        landmarkSet = new LinkedHashSet<>();
        landmarksDistancesBFS = new HashMap<>();
        landmarksDistancesActual = new HashMap<>();
    }

    public void updateProgress() {
        if (progressListener == null) {
            return;
        }
        if (progressedIndicator <= landmarkSet.size()) {
            progressedIndicator = landmarkSet.size();
            if (progressedIndicator <= 16) {
                progressListener.accept(progressedIndicator, 16.0);
            }
        }
    }

    public void setProgressListener(BiConsumer<Double, Double> progressListener) {
        this.progressListener = progressListener;
    }

    public Set<Integer> landmarksMaxCover(int goalAmount, boolean calledAsSubRoutine) {
        if (landmarkSet.size() == goalAmount) {
            return landmarkSet;
        }
        int edgesInGraph = sumEdgesInGraph(graph);
        landmarksAvoid(goalAmount, false);
        Set<Integer> candidateSet = new HashSet<>(landmarkSet);
        int avoidCall = 1;
        while (candidateSet.size() < 4 * goalAmount && avoidCall < goalAmount * 5) {
            if (progressListener != null) {
                if (candidateSet.size() / (4 * goalAmount) > avoidCall / (goalAmount * 5)) {
                    progressListener.accept(candidateSet.size() * 1.00, 4.0 * goalAmount);
                } else {
                    progressListener.accept(avoidCall * 1.00, 5.0 * goalAmount);
                }
            }

            removeRandomCandidateAndGraphMarks(landmarkSet);
            landmarksAvoid(goalAmount, true);
            avoidCall++;
            candidateSet.addAll(landmarkSet);
        }
        // Approximate log2(goal+1)
        int flooredLog = (int) (Math.log(goalAmount) / Math.log(2)) + 1;
        if (progressListener != null) progressListener.accept(1.0, 100.0);
        HashMap<Set<Integer>, Integer> setCoverMap = new HashMap<>();
        for (int i = 0; i < flooredLog; i++) {
            List<Integer> candidateSubSetList = new ArrayList<>(candidateSet);
            while (candidateSubSetList.size() > goalAmount) {
                Collections.shuffle(candidateSubSetList);
                candidateSubSetList.remove(0);
            }
            Set<Integer> candidateSubSet = new HashSet<>(candidateSubSetList);
            boolean improveFound = true;
            double approximateProgress = i;
            Map<Edge, Set<Integer>> coveredEdges = calculateCoveredEdges(landmarkSet);
            int currentProfit = coveredEdges.size();
            while (improveFound) {
                List<int[]> profitStore = new ArrayList<>();
                if (progressListener != null)
                    progressListener.accept(Math.min((double) i + 1, approximateProgress), (double) flooredLog);
                improveFound = false;
            /*    int bestSwapCandidateIn = -1;
                int bestSwapCandidateOut = -1;*/
                for (Integer outCandidate : landmarkSet) {
                    for (Integer swapCandidate : candidateSubSet) {
                        Map<Edge, Set<Integer>> coveredEdgesWithoutOut = coveredEdgesWithout(coveredEdges, outCandidate);
                        int withoutSize = coveredEdgesWithoutOut.size();
                        Map<Edge, Set<Integer>> coveredEdgesWithIn = coveredEdgesWithLandmark(coveredEdgesWithoutOut, swapCandidate);
                        int withInSize = coveredEdgesWithIn.size();
                        assert withInSize >= withoutSize;
                        int newValue = coveredEdgesWithIn.size();
                        if (newValue > currentProfit) {
                            int[] store = new int[3];
                            store[0] = outCandidate;
                            store[1] = swapCandidate;
                            store[2] = newValue;
                            profitStore.add(store);
                           /* bestSwapCandidateOut = outCandidate;
                            bestSwapCandidateIn = swapCandidate;
                            currentProfit = newValue;*/
                            improveFound = true;
                        }
                    }
                }
               /* if (bestSwapCandidateIn != -1) {
                    candidateSubSet.remove(bestSwapCandidateOut);
                    candidateSubSet.add(bestSwapCandidateIn);
                    candidateSet.remove(bestSwapCandidateIn);
                    candidateSet.add(bestSwapCandidateOut);
                }*/
                if (profitStore.size() > 0) {
                    int[] probabilitySum = new int[profitStore.size()];
                    probabilitySum[0] = profitStore.get(0)[2] - currentProfit;
                    for (int j = 1; j < probabilitySum.length; j++) {
                        probabilitySum[j] = profitStore.get(j)[2] + probabilitySum[j - 1] - currentProfit;
                    }
                    Random r = new Random();
                    int lottoWinner = r.nextInt(probabilitySum[probabilitySum.length - 1]);
                    int winningSwap = -1;
                    if (0 <= lottoWinner && lottoWinner < probabilitySum[0]) winningSwap = 0;
                    for (int j = 1; j < probabilitySum.length; j++) {
                        if (probabilitySum[j - 1] <= lottoWinner && lottoWinner < probabilitySum[j]) winningSwap = j;
                    }
                    currentProfit = profitStore.get(winningSwap)[2];
                    landmarkSet.remove(profitStore.get(winningSwap)[0]);
                    landmarkSet.add(profitStore.get(winningSwap)[1]);
                    candidateSet.remove(profitStore.get(winningSwap)[1]);
                    candidateSet.add(profitStore.get(winningSwap)[0]);
                    coveredEdges = calculateCoveredEdges(candidateSubSet);
                }
                approximateProgress += 0.1;
            }
            setCoverMap.put(new HashSet<>(landmarkSet), currentProfit);
            if (progressListener != null) progressListener.accept((double) i, (double) flooredLog);
        }
        if (progressListener != null) progressListener.accept(1.00, 1.00);
        landmarksDistancesActual.clear();
        int best = 0;
        for (Map.Entry<Set<Integer>, Integer> ent : setCoverMap.entrySet()) {
            if (ent.getValue() > best) {
                landmarkSet = ent.getKey();
                best = ent.getValue();
            }
        }
        return landmarkSet;
    }

    private int sumEdgesInGraph(Graph graph) {
        int counter = 0;
        for (int i = 0; i < graph.getNodeList().size(); i++) {
            for (Edge e : graph.getAdjList().get(i)) {
                counter++;
            }
        }
        return counter;
    }

    private Map<Edge, Set<Integer>> coveredEdgesWithout(Map<Edge, Set<Integer>> edgeCoveredMap, Integer outCandidate) {
        Iterator<Map.Entry<Edge, Set<Integer>>> iterator = edgeCoveredMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Edge, Set<Integer>> entry = iterator.next();
            Set<Integer> temp = entry.getValue();
            temp.remove(outCandidate);
            if (temp.isEmpty()) {
                iterator.remove();
            }
        }
        return edgeCoveredMap;
    }

    private Map<Edge, Set<Integer>> coveredEdgesWithLandmark(Map<Edge, Set<Integer>> edgeCoveredMap, Integer inCandidate) {
        List<List<Edge>> originalList = graph.getAdjList();
        List<Double> forwardDistance = getNodeDistanceLandmark(inCandidate);
        double[] distances = forwardDistance.stream().mapToDouble(Double::doubleValue).toArray();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            for (Edge e : originalList.get(i)) {
                if (distances[e.to] == Double.MAX_VALUE || distances[i] == Double.MAX_VALUE) {
                    continue;
                }
                double a = distances[e.to] - distances[i];
                double abs = Math.abs((distances[i] - distances[e.to]) - e.d);
                if (abs <= 0.000000000000001 && e.d != 0) {
                    Set<Integer> edgedCoveredSet = edgeCoveredMap.computeIfAbsent(e, k -> new HashSet<>());
                    edgedCoveredSet.add(inCandidate);
                    edgeCoveredMap.replace(e, edgedCoveredSet);
                }
            }
        }
        return edgeCoveredMap;
    }

    private int calculateCoverCost(Set<Integer> potentialLandmarks) {
        int covers = 0;
        Map<Integer, double[]> distanceMap = new HashMap<>();
        List<List<Edge>> originalList = graph.getAdjList();
        for (Integer lMarkIndex : potentialLandmarks) {
            List<Double> forwardDistance = getNodeDistanceLandmark(lMarkIndex);
            double[] arrForward = forwardDistance.stream().mapToDouble(Double::doubleValue).toArray();
            distanceMap.put(lMarkIndex, arrForward);
        }
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            for (Edge e : originalList.get(i)) {
                for (Integer lMarkIndex : potentialLandmarks) {
                    double[] distances = distanceMap.get(lMarkIndex);
                    if (distances[e.to] == Double.MAX_VALUE || distances[i] == Double.MAX_VALUE) {
                        continue;
                    }
                    double a = distances[e.to] - distances[i];
                    if (Math.abs((e.d - distances[e.to] + distances[i]) - 0.0) <= Math.ulp(0.0)) {
                        covers++;
/*
                        System.out.println("Edge from: " + i + " to: " + lMarkIndex + " is covered");
*/
                        break;
                    }
                }
            }
        }
        return covers;
    }

    private Map<Edge, Set<Integer>> calculateCoveredEdges(Set<Integer> potentialLandmarks) {
        Map<Edge, Set<Integer>> coveredEdges = new HashMap<>();
        Map<Integer, double[]> distanceMap = new HashMap<>();
        List<List<Edge>> originalList = graph.getAdjList();
        for (Integer lMarkIndex : potentialLandmarks) {
            List<Double> forwardDistance = getNodeDistanceLandmark(lMarkIndex);
            double[] arrForward = forwardDistance.stream().mapToDouble(Double::doubleValue).toArray();
            distanceMap.put(lMarkIndex, arrForward);
        }
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            for (Edge e : originalList.get(i)) {
                for (Integer lMarkIndex : potentialLandmarks) {
                    double[] distances = distanceMap.get(lMarkIndex);
                    if (distances[e.to] == Double.MAX_VALUE || distances[i] == Double.MAX_VALUE) {
                        continue;
                    }
                    double a = distances[i] - distances[e.to];
                    double abs = Math.abs((distances[e.to] - distances[i]) - e.d);
                    if (abs <= 0.000000000000001 && e.d != 0) {
                        Set<Integer> edgedCoveredSet = coveredEdges.computeIfAbsent(e, k -> new HashSet<>());
                        edgedCoveredSet.add(lMarkIndex);
                        coveredEdges.replace(e, edgedCoveredSet);
                        /*
                        System.out.println("Edge from: " + i + " to: " + lMarkIndex + " is covered");
*/
                    }
                }
            }
        }
        return coveredEdges;
    }

    private List<Double> getNodeDistanceLandmark(int lm) {
        landmarksDistancesActual.computeIfAbsent(lm, k -> SSSP.singleToAllPath(k).nodeDistances);
        return landmarksDistancesActual.get(lm);
    }

    private int maxCoverCostLandmark(Integer lMarkIndex) {
        int covers = 0;
        List<List<Edge>> originalList = graph.getAdjList();
        List<Double> forwardDistance = getNodeDistanceLandmark(lMarkIndex);
        double[] distances = forwardDistance.stream().mapToDouble(Double::doubleValue).toArray();
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            for (Edge e : originalList.get(i)) {
                if (distances[e.to] == Double.MAX_VALUE || distances[i] == Double.MAX_VALUE) {
                    continue;
                }
                if (Math.abs((e.d - distances[e.to] + distances[i]) - 0.0) <= Math.ulp(0.0)) {
                    covers++;
                }
            }
        }
        return covers;
    }

    private void removeRandomCandidateAndGraphMarks(Set<Integer> candidateSet) {
        Iterator<Integer> iterator = candidateSet.iterator();
        while (iterator.hasNext()) {
            int temp = (Math.random() <= 0.5) ? 1 : 2;
            iterator.next();
            if (temp == 1) {
                iterator.remove();
            }
        }
    }


    public Set<Integer> landmarksAvoid(int goalAmount, boolean calledAsSubroutine) {
        SSSP.setLandmarkArray(null);
        if (landmarkSet == null || landmarkSet.isEmpty()) {
            Random random = new Random();
            int randomInitialLandmark = random.nextInt(graph.getNodeAmount());
            landmarkSet.add(randomInitialLandmark);
            landmarksDistancesBFS.put(randomInitialLandmark, new ModelUtil(graph).bfsMaxDistance(randomInitialLandmark));
            avoidGetLeaf();
            SSSP.setLandmarkArray(null);
            landmarkSet.remove(randomInitialLandmark);
        }

        while (landmarkSet.size() < goalAmount) {
            if (!calledAsSubroutine) updateProgress();
            avoidGetLeaf();
        }
        if (!calledAsSubroutine) updateProgress();
        return landmarkSet;
    }

    private void avoidGetLeaf() {
        // TODO: 16-03-2020 Avoid trapping in one-way streets, possibly by maxCover or just hack our way out of it
        int rootNode = new Random().nextInt(graph.getNodeAmount());
        ShortestPathResult SPT = SSSP.singleToAllPath(rootNode);
        double[] weightedNodes = new double[graph.getNodeAmount()];
        SSSP.applyFactory(new LandmarksFactory());
        HeuristicFunction S = SSSP.getHeuristicFunction();
        for (int i = 0; i < SPT.nodeDistances.size(); i++) {
            double heuristicVal = S.apply(rootNode, i);
            double val = SPT.nodeDistances.get(i) - heuristicVal;
            if (val < 0.000000000000001) {
                val = 0;
            }
            weightedNodes[i] = val;
        }
        Map<Integer, List<Integer>> pathInversed = SPT.pathMap.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        double maxWeight = -1.0;
        int bestCandidate = 0;
        double[] sizeNodes = new double[graph.getNodeAmount()];
        for (int i = 0; i < graph.getNodeAmount(); i++) {
            if (i == rootNode) {
                continue;
            }
            double weighSumNode = findSumInTree(i, pathInversed, weightedNodes);
            if (weighSumNode == -1.0) {
                weighSumNode = 0.0;
            } ;
            sizeNodes[i] = weighSumNode;
            if (maxWeight <= weighSumNode) {
                bestCandidate = i;
                maxWeight = weighSumNode;
            }
        }
        int leaf = findLeaf(bestCandidate, pathInversed, sizeNodes);
        landmarkSet.add(leaf);
    }

    private Integer getLatestLandMark() {
        if (landmarkSet.isEmpty()) {
            return null;
        }
        Iterator<Integer> iterator = landmarkSet.iterator();
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
        if (landmarkSet.contains(treeRoot)) {
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


    public Set<Integer> landmarksFarthest(int goalAmount, boolean calledAsSubroutine) {
        // Current implementation is 'FarthestB' (B - breadth)
        // Simple but not necessarily best. MaxCover yields better results.
        // TODO: MaxCover for landmark selection
        ModelUtil gu = new ModelUtil(graph);

        if (landmarkSet.isEmpty()) {
            Random random = new Random();
            random.setSeed(666974757);
            int startNode = random.nextInt(graph.getNodeAmount());
            int[] arr = gu.bfsMaxDistance(startNode);
            int max = 0;
            for (int i = 0; i < arr.length; i++) {
                max = arr[i] > arr[max] ? i : max;
            }
            landmarksDistancesBFS.put(startNode, arr);
            landmarkSet.add(max);
        }

        while (landmarkSet.size() < goalAmount) {
            updateProgress();
            int furthestCandidate = getFurthestCandidateLandmark();
            landmarksDistancesBFS.put(furthestCandidate, gu.bfsMaxDistance(furthestCandidate));
            landmarkSet.add(furthestCandidate);
        }
        updateProgress();
        return landmarkSet;
    }

    private int getFurthestCandidateLandmark() {
        int highestMinimal = 0;
        int furthestCandidate = 0;
        for (Node n : graph.getNodeList()) {
            if (landmarkSet.contains(n.index)) continue;
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

    public Set<Integer> getLandmarkSet() {
        return landmarkSet;
    }


    public Set<Integer> landmarksRandom(int i, boolean calledAsSubRoutine) {
        while (landmarkSet.size() < i) {
            updateProgress();
            landmarkSet.add(new Random().nextInt(graph.getNodeAmount()));
        }
        updateProgress();
        return landmarkSet;
    }

    public BiFunction<Integer, Boolean, Set<Integer>> getAvoidFunction(Landmarks lm) {
        return lm::landmarksAvoid;
    }

    public BiFunction<Integer, Boolean, Set<Integer>> getFarthestFunction(Landmarks lm) {
        return lm::landmarksFarthest;
    }

    public BiFunction<Integer, Boolean, Set<Integer>> getRandomFunction(Landmarks lm) {
        return lm::landmarksRandom;
    }

    public BiFunction<Integer, Boolean, Set<Integer>> getMaxCover(Landmarks lm) {
        return lm::landmarksMaxCover;
    }

    public void clearLandmarks() {
        landmarkSet.clear();
    }

    public void setLandmarkSet(Set<Integer> landmarksSet) {
        landmarkSet = landmarksSet;
    }
}
