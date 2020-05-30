package paths.generator;

import datastructures.DuplicatePriorityQueueNode;
import datastructures.JavaDuplicateMinPriorityQueue;
import paths.strategy.QueuePollingStrategy;
import paths.strategy.QueueUpdatingStrategy;

import static paths.ABDir.A;
import static paths.SSSP.*;

public class QueueUpdateGenerator {
    public static QueueUpdatingStrategy getRegularStrategy() {
        return (toUpdate, dir) -> {
            if (dir == A) getHeuristicValuesA()[toUpdate] = getPriorityStrategyA().apply(toUpdate, dir);
            else getHeuristicValuesB()[toUpdate] = getPriorityStrategyB().apply(toUpdate, dir);
            getQueue(dir).updatePriority(toUpdate);
        };
    }

    public static QueueUpdatingStrategy getDuplicateStrategy() {
        return (toUpdate, dir) -> {
            if (dir == A) {
                DuplicatePriorityQueueNode dupNode = new DuplicatePriorityQueueNode(toUpdate, getPriorityStrategyA().apply(toUpdate, dir));
                getQueue(dir).insert(dupNode);
            } else {
                DuplicatePriorityQueueNode dupNode = new DuplicatePriorityQueueNode(toUpdate, getPriorityStrategyB().apply(toUpdate, dir));
                getQueue(dir).insert(dupNode);
            }
        };
    }

}
