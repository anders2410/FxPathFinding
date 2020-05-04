package datastructures;

import java.util.Comparator;
import java.util.TreeSet;

public class TreeSetMinPriorityQueue extends TreeSet<Integer> implements MinPriorityQueue {

    public TreeSetMinPriorityQueue(Comparator<? super Integer> comparator, int graph) {
        super(comparator);
    }

    @Override
    public boolean contains(Integer toFind) {
        return super.contains(toFind);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Integer peek() {
        return super.first();
    }

    @Override
    public Integer poll() {
        return super.pollFirst();
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
