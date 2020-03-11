package paths;

import model.Node;
import model.Util;

import java.util.List;
import java.util.function.BiFunction;

public class HeuristicGenerator {

    public static HeuristicFunction getDistance() {
        return (from, to) -> {
            List<Node> nodeList = SSSP.getGraph().getNodeList();
            return SSSP.getDistanceStrategy().apply(nodeList.get(from), nodeList.get(to));
        };
    }

    public static HeuristicFunction getLandmarks() {
        return (from, to) -> {
            double maxValue = 0;
            //32 because |landmarks| = 16. *2 for 2 ways.
            double[][] landmarkArray = SSSP.getLandmarkArray();
            for (int i = 0; i < 32; i++) {
                double valueForward = landmarkArray[i][to] - landmarkArray[i][from];
                double valueBackward = landmarkArray[i + 1][from] - landmarkArray[i + 1][to];
                if (Math.max(valueBackward, valueForward) > maxValue) {
                    maxValue = Math.max(valueBackward, valueForward);
                }
                i++;
            }
            return maxValue;
        };
    };
}
