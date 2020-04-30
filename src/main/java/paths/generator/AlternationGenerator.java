package paths.generator;

import paths.strategy.AlternationStrategy;

import static paths.ABDir.*;
import static paths.SSSP.*;

public class AlternationGenerator {

    public static AlternationStrategy getOneDirectional() {
        return () -> true;
    }

    public static AlternationStrategy getReverseOneDirectional() {
        return () -> false;
    }

    public static AlternationStrategy getBiggestQueueStrategy() {
        return () -> getQueue(A).size() > getQueue(B).size();
    }

    public static AlternationStrategy getAmountSeenStrategy() {
        return () -> getQueue(A).size() + getScanned(A).size() < getQueue(B).size() + getScanned(B).size();
    }

    public static AlternationStrategy getSameDistanceStrategy() {
        return () -> getNodeDist(A).get(getQueue(A).peek()) < getNodeDist(B).get(getQueue(B).peek());
    }
}
