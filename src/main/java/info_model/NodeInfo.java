package info_model;

import java.io.Serializable;

public class NodeInfo implements Serializable {
    private int index;
    private float natureValue;
    private boolean fuelAmenity;

    public NodeInfo(int index, float natureValue, boolean fuelAmenity) {
        this.index = index;
        this.natureValue = natureValue;
        this.fuelAmenity = fuelAmenity;
    }

    public int getIndex() {
        return index;
    }

    public float getNatureValue() {
        return natureValue;
    }

    public boolean isFuelAmenity() {
        return fuelAmenity;
    }
}
