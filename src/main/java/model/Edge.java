package model;

import java.io.Serializable;
import java.util.Objects;

public class Edge implements Serializable {
    // We should define our own ID as different machines could generate different ID's
    private static final long serialVersionUID = 6529685098267757690L;

    public int from, to;
    public double d;

    public Edge(int from, int to, double d) {
        this.from = from;
        this.to = to;
        this.d = d;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Edge)) return false;
        Edge e = (Edge) obj;
        return Double.compare(e.d, this.d) == 0 && this.to == e.to;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "from=" + from +
                ", to=" + to +
                ", d=" + d +
                "}";
    }
}
