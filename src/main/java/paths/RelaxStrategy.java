package paths;

import model.Edge;

public interface RelaxStrategy {
    void relax(int from, Edge edge, ABDir dir);
}
