package paths.factory;

import paths.*;

import java.util.function.Function;

public interface AlgorithmFactory {
    boolean isBiDirectional();

    PriorityStrategy getPriorityStrategy();

    HeuristicFunction getHeuristicFunction();

    RelaxStrategy getRelaxStrategy();

    TerminationStrategy getTerminationStrategy();
}
