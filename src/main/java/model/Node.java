package model;

import java.io.Serializable;

public class Node implements Serializable {
    public int index;
    public double longitude;
    public double latitude;

    private boolean contracted;
    private int importance;
    private int contractedNeighbours;

    public Node(int index, double longitude, double latitude) {
        this.index = index;
        this.latitude = latitude;
        this.longitude = longitude;
        contracted = false;
        importance = 0;
        contractedNeighbours = 0;
    }

    public Node(Node node) {
        this.index = node.index;
        this.latitude = node.latitude;
        this.longitude = node.longitude;
        contracted = false;
        importance = 0;
        contractedNeighbours = 0;
    }

    public boolean isContracted() {
        return contracted;
    }

    public void setContracted(boolean contracted) {
        this.contracted = contracted;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public int getContractedNeighbours() {
        return contractedNeighbours;
    }

    public void setContractedNeighbours(int contractedNeighbours) {
        this.contractedNeighbours = contractedNeighbours;
    }

    @Override
    public String toString() {
        return "Node{" +
                "index='" + index +
                ", latitude='" + latitude +
                ", longitude=" + longitude +
                "}\n";
    }
}
