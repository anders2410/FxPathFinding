package datastructures;

import paths.SSSP;

import java.util.Comparator;
import java.util.PriorityQueue;

import static paths.ABDir.A;

public class JavaDuplicateMinPriorityQueue extends PriorityQueue<DuplicatePriorityQueueNode> implements MinPriorityQueue {

    private static Comparator<? super DuplicatePriorityQueueNode> duplicateComparator = (i, j) -> {
        double diff = Math.abs(i.value - j.value);
        if (diff <= 0.000000000000001) {
            return i.index.compareTo(j.index);
        } else {
            return Double.compare(i.value, j.value);
        }
    };

    public JavaDuplicateMinPriorityQueue(Comparator<? super DuplicatePriorityQueueNode> comparator, int graph) {
        super(duplicateComparator);
    }

    public JavaDuplicateMinPriorityQueue(JavaDuplicateMinPriorityQueue q) {
        super(q);
    }

    @Override
    public Integer nodePeek() {
        DuplicatePriorityQueueNode peeked = super.peek();
        if (peeked == null) return null;
        return peeked.index;
    }

    @Override
    public Integer nodePoll() {
        DuplicatePriorityQueueNode polled = super.poll();
        if (polled == null) return null;
        return polled.index;
    }

    @Override
    public void insert(DuplicatePriorityQueueNode n) {
        super.add(n);
    }

    @Override
    public boolean contains(Integer toFind) {
        return false;
    }

    @Override
    public void insert(Integer toAdd) {
        super.add(new DuplicatePriorityQueueNode(toAdd, 0));
    }

    @Override
    public void remove(Integer toDelete) {

    }

    @Override
    public void updatePriority(Integer toUpdate) {

    }
}
