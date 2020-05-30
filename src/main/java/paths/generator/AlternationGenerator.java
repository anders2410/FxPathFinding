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

    public static AlternationStrategy getSmallestQueueStrategy() {
        return () -> getQueue(A).size() < getQueue(B).size();
    }

    public static AlternationStrategy getAmountSeenStrategy() {
        return () -> getQueue(A).size() + getScanned(A).size() < getQueue(B).size() + getScanned(B).size();
    }

    public static AlternationStrategy getDensityBasedStrategy() {
        return () -> {
            Integer aPeek = getQueue(A).nodePeek(), bPeek = getQueue(B).nodePeek();
            if (aPeek == null) return false;
            if (bPeek == null) return true;
            return getDensityMeasuresNorm().get(aPeek) < getDensityMeasuresNorm().get(bPeek);
        };
    }

    public static AlternationStrategy getDensityTimesAmountSeenStrategy(double densityInfluence) {
        return () -> {
            Integer aPeek = getQueue(A).nodePeek(), bPeek = getQueue(B).nodePeek();
            if (aPeek == null) return false;
            if (bPeek == null) return true;
            double measureA = getQueue(A).size() + getScanned(A).size()*(1 - densityInfluence + densityInfluence * getDensityMeasuresNorm().get(aPeek));
            double measureB = getQueue(B).size() + getScanned(B).size()*(1 - densityInfluence + densityInfluence * getDensityMeasuresNorm().get(bPeek));
            return measureA < measureB;
        };
    }

    public static AlternationStrategy getSameDistanceStrategy() {
        return () -> {
            Integer aPeek = getQueue(A).nodePeek(), bPeek = getQueue(B).nodePeek();
            if (aPeek == null) return false;
            if (bPeek == null) return true;
            return getNodeDist(A).get(aPeek) < getNodeDist(B).get(bPeek);
        };
    }
}
