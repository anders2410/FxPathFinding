package paths.generator;

import model.Edge;
import model.Graph;
import paths.SSSP;
import paths.strategy.PreprocessStrategy;

import java.util.List;
import java.util.Set;

import static paths.SSSP.*;

public class PreProcessGenerator {

    public static PreprocessStrategy getReachPreStrategy() {
        return () -> {
            if (SSSP.getReachBounds() == null) {
                System.out.println("No reach bounds found for graph");
            }
        };
    }

    public static PreprocessStrategy getRealPreStrategy() {
        return () -> {
            if (SSSP.getReachBounds() == null) {
                System.out.println("No reach bounds found for graph");
                return;
            }
            getLandmarksPreStrategy().process();
        };
    }

    public static PreprocessStrategy getLandmarksPreStrategy() {
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
