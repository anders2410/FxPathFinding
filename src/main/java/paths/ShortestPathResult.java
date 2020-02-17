package paths;

import java.util.List;

public class ShortestPathResult {
    public double d;
    public List<Integer> path;

    public ShortestPathResult(double d, List<Integer> path) {
        this.d = d;
        this.path = path;
    }
}
