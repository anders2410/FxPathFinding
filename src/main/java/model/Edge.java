package model;

import java.io.Serializable;

public class Edge implements Serializable {

    public int to;
    public double d;

    public Edge(int to, double d) {
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
                "to=" + to +
                ", d=" + d +
                "}";
    }
}
