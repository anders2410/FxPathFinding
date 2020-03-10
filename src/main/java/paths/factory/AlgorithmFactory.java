package paths.factory;

import paths.DirAB;
import paths.HeuristicFunction;
import paths.RelaxStrategy;
import paths.TerminationStrategy;

import java.util.function.Function;

public interface AlgorithmFactory {
    boolean isBiDirectional();

    Function<Integer, Double> getPriorityStrategy(DirAB dir);

    HeuristicFunction getHeuristicFunction();

    RelaxStrategy getRelaxStrategy(DirAB dir);

    TerminationStrategy getTerminationStrategy();
}
