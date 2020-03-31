package datastructures;

public abstract interface MinPriorityQueue {

    public abstract boolean contains(Integer toFind);

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract Integer peek();

    public abstract Integer poll();

    public abstract void insert(Integer toAdd);

    public abstract void remove(Integer toDelete);

    public abstract void updatePriority(Integer toUpdate);

}
