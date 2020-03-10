package paths.factory;

import paths.HeuristicFunction;
import paths.RelaxStrategy;
import paths.TerminationStrategy;

import java.util.function.Function;

public interface AlgorithmFactory {
    boolean isBiDirectional();

    Function<Integer, Double> getPriorityStrategy(boolean isForward);

    HeuristicFunction getHeuristicFunction();

    RelaxStrategy getRelaxStrategy();

    TerminationStrategy getTerminationStrategy();
}
