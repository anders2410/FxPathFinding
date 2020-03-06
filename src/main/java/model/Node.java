package model;

public class Node {
    public int index;
    public double longitude;
    public double latitude;

    public Node(int index, double longitude, double latitude) {
        this.index = index;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "Node(" + index + ", " + longitude + ", " + latitude + ")";
    }
}
