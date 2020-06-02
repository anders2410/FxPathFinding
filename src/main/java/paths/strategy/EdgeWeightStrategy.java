package paths.strategy;

import model.Edge;
import model.Node;
import paths.ABDir;

public interface EdgeWeightStrategy {
    double getWeight(Edge edge, ABDir dir);

    double lowerBoundDistance(Node node1, Node node2);

    String getFileSuffix();
}
