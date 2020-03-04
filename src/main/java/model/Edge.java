package model;

import java.util.Comparator;

public class Edge {
    public int to;
    public double d;
    public boolean visited, inPath, isDrawn, visitedReverse;

    public Edge(int to, double d) {
        this.to = to;
        this.d = d;
        visited = false;
        visitedReverse = false;
        inPath = false;
        isDrawn = false;
    }

    public Edge(int to, double d, boolean visited, boolean inPath, boolean isDrawn, boolean visitedReverse) {
        this.to = to;
        this.d = d;
        this.visited = visited;
        this.inPath = inPath;
        this.isDrawn = isDrawn;
        this.visitedReverse = visitedReverse;
    }

    public boolean isBetter(Edge than) {
        return compVal(this) >= compVal(than);
    }

    private int compVal(Edge edge) {
        int compVal = 0;
        if (edge.isDrawn) {
            compVal++;
        }
        if (edge.inPath) {
            compVal++;
        }
        if (edge.visited) {
            compVal++;
        }
        if (edge.visitedReverse) {
            compVal++;
        }
        return compVal;
    }
}
