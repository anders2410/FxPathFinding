package paths.generator;

import model.Edge;
import model.Graph;
import paths.SSSP.*;
import paths.strategy.PreprocessStrategy;

import java.util.List;

import static paths.SSSP.*;

public class LandmarksPreStrategy implements PreprocessStrategy {

    public void process() {
        generateLandmarks();
    }

    private static void generateLandmarks() {
        double[][] landmarkArray = getLandmarkArray();
        Graph graph = getGraph();
        if (landmarkArray == null && !graph.getLandmarks().isEmpty()) {
            landmarkArray = new double[32][graph.getNodeAmount()];
            int index = 0;
            List<List<Edge>> originalList = graph.getAdjList();
            for (Integer landmarkIndex : graph.getLandmarks()) {
                List<Double> forwardDistance = singleToAllPath(graph, landmarkIndex).nodeDistance;
                double[] arrForward = forwardDistance.stream().mapToDouble(Double::doubleValue).toArray();
                graph.setAdjList(graph.reverseAdjacencyList(graph.getAdjList()));
                List<Double> backDistance = singleToAllPath(graph, landmarkIndex).nodeDistance;
                double[] arrBackward = backDistance.stream().mapToDouble(Double::doubleValue).toArray();
                graph.setAdjList(originalList);
                landmarkArray[index] = arrForward;
                landmarkArray[index + 1] = arrBackward;
                index++;
                index++;
            }
            graph.resetPathTrace();
        }
        setLandmarkArray(landmarkArray);
    }
}
