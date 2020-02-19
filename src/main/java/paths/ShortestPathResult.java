package paths;

import java.util.List;

public class ShortestPathResult {
    public double d;
    public List<Integer> path;
    public int seenNodes;

    public ShortestPathResult(double d, List<Integer> path, int seenNodes) {
        this.d = d;
        this.path = path;
        this.seenNodes = seenNodes;
    }
}
