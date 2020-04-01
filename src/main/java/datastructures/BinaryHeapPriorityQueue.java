package datastructures;

import java.util.Arrays;
import java.util.Comparator;

public class BinaryHeapPriorityQueue implements MinPriorityQueue {

    private final int defaultElement;
    private Comparator<? super Integer> comparator;
    private int size;

    // TODO: Array or ArrayList?
    // With array we initialise to full capacity meaning we have an array of the graph's size of ints,
    // but with array list we do costly calls to double size underway, so which is more efficient?

    private int[] binHeap;

    public BinaryHeapPriorityQueue(Comparator<? super Integer> comparator, int graph) {
        size = 0;
        binHeap = new int[graph + 1];
        defaultElement = -1;
        Arrays.fill(binHeap, defaultElement);
        this.comparator = comparator;
    }

    @Override
    public boolean contains(Integer toFind) {
        for (Integer i : binHeap) {
            if (i.equals(toFind)) return true;
        }
        return false;
    }

    public int find(Integer toFind) {
        int index = 0;
        for (Integer i : binHeap) {
            if (i.equals(toFind)) return index;
            index++;
        }
        return -1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == binHeap.length;
    }

    public void reset() {
        size = 0;
    }

    @Override
    public Integer peek() {
        return binHeap[0];
    }

    @Override
    public Integer poll() {
        int min = binHeap[0];
        delete(0);
        return min;
    }

    @Override
    public void insert(Integer toAdd) {
        binHeap[size++] = toAdd;
        bubbleUp(size - 1);
    }

    @Override
    public void remove(Integer toDelete) {
        for (int i = 0; i < size; i++) {
            if (binHeap[i] == toDelete) {
                delete(i);
                break;
            }
        }

    }

    public int delete(Integer indexToDelete) {
        int elem = binHeap[indexToDelete];
        binHeap[indexToDelete] = binHeap[size - 1];
        size--;
        bubbleDown(indexToDelete);
        return elem;
    }

    @Override
    public void updatePriority(Integer toUpdate) {
        int index = find(toUpdate);
        if (index == 0) {
            bubbleDown(index);
        } else if (index != -1) {
            if (0 > comparator.compare(toUpdate, binHeap[parent(index)])) {
                bubbleUp(index);
            } else {
                bubbleDown(index);
            }
        } else {
            insert(toUpdate);
        }
    }

    private int parent(int child) {
        return (child - 1) / 2;
    }

    private int child(int parent, int xthChild) {
        return 2 * parent + xthChild;
    }

    private void bubbleUp(int child) {
        int toMove = binHeap[child];
        //Comparator returns -1 if first object is < than second, giving second condition in while loop
        while (child > 0 && 0 > comparator.compare(toMove, binHeap[parent(child)])) {
            binHeap[child] = binHeap[parent(child)];
            child = parent(child);
        }
        binHeap[child] = toMove;
    }

    private void bubbleDown(int bubbleDownElem) {
        int toMove = binHeap[bubbleDownElem];
        int child;
        while (child(bubbleDownElem, 1) < size) {
            child = minChild(bubbleDownElem);
            if (0 > comparator.compare(binHeap[child], toMove)) {
                binHeap[bubbleDownElem] = binHeap[child];
            } else {
                break;
            }
            bubbleDownElem = child;
        }
        binHeap[bubbleDownElem] = toMove;
    }

    private int minChild(int parent) {
        //Smallest in the sense of index
        int smallestChild = child(parent, 1);
        int secondSmallestChild = child(parent, 2);
        if (0 > comparator.compare(binHeap[secondSmallestChild], binHeap[smallestChild])) {
            smallestChild = secondSmallestChild;
        }

        return smallestChild;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Heap: ");
        for (int i = 0; i < size; i++) {
            s.append(binHeap[i]).append(" ");
        }
        return s.toString();
    }
}
