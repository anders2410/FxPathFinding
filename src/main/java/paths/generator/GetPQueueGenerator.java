package paths.generator;

import datastructures.BinaryHeapPriorityQueue;
import datastructures.JavaMinPriorityQueue;
import datastructures.TreeSetMinPriorityQueue;
import paths.strategy.GetPQueueStrategy;

public class GetPQueueGenerator {

    public static GetPQueueStrategy getJavaQueue() {
        return JavaMinPriorityQueue::new;
    }

    public static GetPQueueStrategy getTreeQueue() {
        return TreeSetMinPriorityQueue::new;
    }

    public static GetPQueueStrategy getBinHeapQueue() {
        return BinaryHeapPriorityQueue::new;
    }
}
