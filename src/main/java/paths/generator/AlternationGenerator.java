package paths.generator;

import paths.SSSP;
import paths.strategy.AlternationStrategy;
import paths.SSSP.*;

import static paths.ABDir.*;
import static paths.SSSP.*;

public class AlternationGenerator {

    public static AlternationStrategy getOneDirectional() {
        return () -> true;
    }

    public static AlternationStrategy getAmountSeenStrategy() {
        return () -> getQueue(A).size() + getVisited(A).size() < getQueue(B).size() + getVisited(B).size();
    }

    public static AlternationStrategy getSameDistanceStrategy() {
        return () -> getNodeDist(A).get(getQueue(A).peek()) < getNodeDist(B).get(getQueue(B).peek());
    }
}
