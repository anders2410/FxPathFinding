package paths.generator;

import datastructures.BinaryHeapPriorityQueue;
import datastructures.JavaMinPriorityQueue;
import paths.strategy.GetPQueueStrategy;

import java.util.Comparator;

public class GetPQueueGenerator {

    public static GetPQueueStrategy getJavaQueue() {
        return JavaMinPriorityQueue::new;
    }

    public static GetPQueueStrategy getBinHeapQueue() {
        return (comp, size) -> {
            return new BinaryHeapPriorityQueue(comp, size);
        };
    }
}
