package datastructures;

import model.Graph;

import java.util.Comparator;
import java.util.PriorityQueue;

public class JavaMinPriorityQueue extends PriorityQueue<Integer> implements MinPriorityQueue {

    public JavaMinPriorityQueue(Comparator<? super Integer> comparator, int graph) {
        super(comparator);
    }

    public JavaMinPriorityQueue(JavaMinPriorityQueue integers) {
        super(integers);
    }

    @Override
    public boolean contains(Integer toFind) {
        return super.contains(toFind);
    }

    @Override
    public Integer nodePeek() {
        return super.peek();
    }

    @Override
    public Integer nodePoll() {
        return super.poll();
    }

    @Override
    public void insert(DuplicatePriorityQueueNode n) {

    }

    @Override
    public void insert(Integer toAdd) {
        super.add(toAdd);
    }

    @Override
    public void remove(Integer toDelete) {
        super.remove(toDelete);
    }

    @Override
    public void updatePriority(Integer toUpdate) {
        super.remove(toUpdate);
        super.add(toUpdate);
    }

    @Override
    public String toString() {
        JavaMinPriorityQueue copy = new JavaMinPriorityQueue(this);
        StringBuilder s = new StringBuilder();
        while (copy.size() != 0) {
            s.append(copy.poll()).append(", ");
        }
        return s.toString();
    }
}
