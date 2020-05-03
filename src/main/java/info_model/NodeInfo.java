package info_model;

import java.io.Serializable;

public class NodeInfo implements Serializable {
    private int index;

    public NodeInfo(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
