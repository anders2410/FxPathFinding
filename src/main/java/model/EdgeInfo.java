package model;

public class EdgeInfo {
    private int from, to;
    private int maxSpeed;

    public EdgeInfo(int from, int to, int maxSpeed) {
        this.from = from;
        this.to = to;
        this.maxSpeed = maxSpeed;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }
}
