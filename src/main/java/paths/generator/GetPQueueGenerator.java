package paths.generator;

import datastructures.JavaMinPriorityQueue;
import datastructures.MinPriorityQueue;
import model.Edge;
import paths.ABDir;
import paths.strategy.GetPQueueStrategy;
import paths.strategy.RelaxStrategy;

import java.util.PriorityQueue;

import static paths.ABDir.A;
import static paths.ABDir.B;
import static paths.SSSP.*;

public class GetPQueueGenerator {

    public static GetPQueueStrategy getJavaQueue() {
        return JavaMinPriorityQueue::new;
    }

    public static RelaxStrategy getDijkstra() {
        return (from, edge, dir) -> {
            edge.visited = true;
            double newDist = getNodeDist(dir).get(from) + edge.d;

            if (newDist < getNodeDist(dir).get(edge.to)) {
                getNodeDist(dir).set(edge.to, newDist);
                getQueue(dir).remove(edge.to);
                getQueue(dir).add(edge.to);
                getPathMap(dir).put(edge.to, from);
                trace(getQueue(dir), dir);
            }
        };
    }
}
