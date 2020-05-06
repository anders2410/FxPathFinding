package info_model;

import java.io.Serializable;

public class EdgeInfo implements Serializable {

    private int from, to;
    private int maxSpeed;
    private Surface surface;

    public EdgeInfo(int from, int to, int maxSpeed, Surface surface) {
        this.from = from;
        this.to = to;
        this.maxSpeed = maxSpeed;
        this.surface = surface;
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

    public Surface getSurface() {
        return surface;
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
