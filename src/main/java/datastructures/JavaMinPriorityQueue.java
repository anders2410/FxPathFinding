package datastructures;

import model.Graph;

import java.util.Comparator;
import java.util.PriorityQueue;

public class JavaMinPriorityQueue extends PriorityQueue<Integer> implements MinPriorityQueue {

    public JavaMinPriorityQueue(Comparator<? super Integer> comparator, int graph) {
        super(comparator);
    }

    @Override
    public boolean contains(Integer toFind) {
        return super.contains(toFind);
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
}
