package paths;

import model.Edge;
import model.Node;

import java.util.AbstractQueue;
import java.util.List;
import java.util.Map;

public interface RelaxStrategy {
    void relax(int from, Edge edge, DirAB dir);
}
