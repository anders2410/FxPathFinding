package info_model;

import java.io.Serializable;

public class EdgeInfo implements Serializable {

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

    @Override
    public String toString() {
        return "Edge{" +
                "from=" + from +
                ", to=" + to +
                ", maxSpeed=" + maxSpeed +
                "}";
    }
}
