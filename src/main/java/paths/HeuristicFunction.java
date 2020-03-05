package paths;

import model.Edge;
import model.Node;

public interface HeuristicFunction {
    double applyHeuristic(Node from, Node target);
}
