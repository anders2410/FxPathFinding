package paths.factory;

import paths.strategy.HeuristicFunction;
import paths.strategy.PriorityStrategy;
import paths.strategy.RelaxStrategy;
import paths.strategy.TerminationStrategy;

public interface AlgorithmFactory {
    boolean isBiDirectional();

    PriorityStrategy getPriorityStrategy();

    HeuristicFunction getHeuristicFunction();

    RelaxStrategy getRelaxStrategy();

    TerminationStrategy getTerminationStrategy();
}
