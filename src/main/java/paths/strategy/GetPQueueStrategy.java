package paths.strategy;

import datastructures.MinPriorityQueue;

import java.util.Comparator;

public interface GetPQueueStrategy {
    MinPriorityQueue initialiseNewQueue(Comparator<? super Integer> comparator, int graph);
}
