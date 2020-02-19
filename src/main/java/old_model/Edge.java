package old_model;

public class Edge {
    public int to;
    public float d;
    public boolean visited, inPath, isDrawn;

    public Edge(int to, float d) {
        this.to = to;
        this.d = d;
        visited = false;
        inPath = false;
        isDrawn = false;
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
        return compVal;
    }
}