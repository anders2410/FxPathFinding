package datastructures;

public class DuplicatePriorityQueueNode {
    Integer index;
    double value;

    public DuplicatePriorityQueueNode(int index, double value) {
        this.index = index;
        this.value = value;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
