package datastructures;

public abstract interface MinPriorityQueue {

    public abstract boolean find(Integer toFind);

    public abstract void insert(Integer toAdd);

    public abstract void delete(Integer toDelete);

    public abstract void updatePriority(Integer toUpdate);

}
