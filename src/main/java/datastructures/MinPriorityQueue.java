package datastructures;

public interface MinPriorityQueue {

    boolean contains(Integer toFind);

    int size();

    boolean isEmpty();

    Integer nodePeek();

    Integer nodePoll();

    void insert(DuplicatePriorityQueueNode n);

    void insert(Integer toAdd);

    void remove(Integer toDelete);

    void updatePriority(Integer toUpdate);

}
