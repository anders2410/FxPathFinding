package paths.strategy;

import model.Edge;
import model.Node;

public interface HeuristicFunction {
    double apply(int from, int to);
}
