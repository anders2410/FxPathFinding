package paths.generator;

import model.Edge;
import model.Graph;
import paths.SSSP;
import paths.strategy.PreProcessStrategy;

import java.util.*;

import static paths.SSSP.*;

public class PreProcessGenerator {

    public static PreProcessStrategy getCHPreStrategy() {
        return () -> {
            List<Integer> ranks = SSSP.getContractionHierarchiesResult().getRanks();
            Graph graph = SSSP.getContractionHierarchiesResult().getGraph();
            SSSP.setCHGraph(graph);
            if (ranks == null || graph == null) {
                System.out.println("Something wrong with CH preprocess..");
            }
        };
    }

    public static PreProcessStrategy getReachPreStrategy() {
        return () -> {
            if (SSSP.getReachBounds() == null) {
                System.out.println("No reach bounds found for graph");
            }
        };
    }

    public static PreProcessStrategy getRealPreStrategy() {
        return () -> {
            if (SSSP.getReachBounds() == null) {
                System.out.println("No reach bounds found for graph");
                return;
            }
            getLandmarksPreStrategy().process();
        };
    }

    public static PreProcessStrategy getLandmarksPreStrategy() {
        return PreProcessGenerator::generateLandmarks;
    }

    private static void generateLandmarks() {
        double[][] landmarkArray = getLandmarkArray();
        int target = getTarget(), source = getSource();
        Graph graph = getGraph();
        Set<Integer> landmarkSet = getLandmarks().getLandmarkSet();

        if ((landmarkArray == null || !landmarkSet.isEmpty() && landmarkArray.length / 2 < landmarkSet.size())) {
            landmarkArray = new double[landmarkSet.size() * 2][graph.getNodeAmount()];
            int index = 0;
            List<List<Edge>> originalList = graph.getAdjList();
            for (Integer landmarkIndex : landmarkSet) {
                List<Double> forwardDistance = singleToAllPath(landmarkIndex).nodeDistances;
                double[] arrForward = forwardDistance.stream().mapToDouble(Double::doubleValue).toArray();
                graph.setAdjList(graph.getReverse(graph.getAdjList()));
                List<Double> backDistance = singleToAllPath(landmarkIndex).nodeDistances;
                double[] arrBackward = backDistance.stream().mapToDouble(Double::doubleValue).toArray();
                graph.setAdjList(originalList);
                landmarkArray[index] = arrForward;
                landmarkArray[index + 1] = arrBackward;
                index += 2;
            }
            assert originalList == graph.getAdjList();
        }
        setLandmarkArray(landmarkArray);
    }
}
