package paths;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public interface TerminationStrategy {
    boolean checkTermination(List<Double> nodeDistA, Map<Integer, Double> estimatedNodeDistA, PriorityQueue<Integer> queueA, List<Double> nodeDistB, Map<Integer, Double> estimatedNodeDistB, PriorityQueue<Integer> queueB, double goalDistance);
}
