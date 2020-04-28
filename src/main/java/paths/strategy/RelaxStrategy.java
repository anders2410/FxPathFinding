package paths.strategy;

import model.Edge;
import paths.ABDir;

public interface RelaxStrategy {
    void relax(Edge edge, ABDir dir);
}
