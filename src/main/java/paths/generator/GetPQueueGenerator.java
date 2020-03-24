package paths.generator;

import datastructures.JavaMinPriorityQueue;
import paths.strategy.GetPQueueStrategy;

public class GetPQueueGenerator {

    public static GetPQueueStrategy getJavaQueue() {
        return JavaMinPriorityQueue::new;
    }

}
