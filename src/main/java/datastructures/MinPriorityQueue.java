package datastructures;

public interface MinPriorityQueue {

    boolean contains(Integer toFind);

    int size();

    boolean isEmpty();

    Integer peek();

    Integer poll();

    void insert(Integer toAdd);

    void remove(Integer toDelete);

    void updatePriority(Integer toUpdate);

}
