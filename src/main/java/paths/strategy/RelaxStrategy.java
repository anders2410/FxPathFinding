package paths.strategy;

import model.Edge;
import paths.ABDir;

public interface RelaxStrategy {
    void relax(int from, Edge edge, ABDir dir);
}
