package model;

import java.io.Serializable;

public class Node implements Serializable {
    // We should define our own ID as different machines could generate different ID's
    private static final long serialVersionUID = 6529685098267757690L;

    public int index;
    public double longitude;
    public double latitude;

    public Node(int index, double longitude, double latitude) {
        this.index = index;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Node(Node node) {
        this.index = node.index;
        this.latitude = node.latitude;
        this.longitude = node.longitude;
    }

    @Override
    public String toString() {
        return "Node{" +
                "index='" + index +
                ", latitude='" + latitude +
                ", longitude=" + longitude +
                "}";
    }
}
