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

}
