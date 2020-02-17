package model;

public class Node {
    public int index;
    public double latitude;
    public double longitude;

    public Node(int index, double latitude, double longitude) {
        this.index = index;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "Node(" + index + ", " + latitude + ", " + longitude + ")";
    }
}
