package paths.generator;

import paths.SSSP;
import paths.strategy.AlternationStrategy;
import paths.SSSP.*;

import static paths.ABDir.*;
import static paths.SSSP.getQueue;
import static paths.SSSP.getVisited;

public class AlternationGenerator {

    public static AlternationStrategy getOneDirectional() {
        return () -> true;
    }

    public static AlternationStrategy getAmountSeenStrategy() {
        return () -> getQueue(A).size() + getVisited(A).size() < getQueue(B).size() + getVisited(B).size();
    }

    public static AlternationStrategy getSameDistanceStrategy() {
        return () -> false;
    }
}
