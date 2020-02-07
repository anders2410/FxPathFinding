package paths;

import java.util.List;

public class ShortestPathResult {
    public float d;
    public List<Integer> path;

    public ShortestPathResult(float d, List<Integer> path) {
        this.d = d;
        this.path = path;
    }
}
