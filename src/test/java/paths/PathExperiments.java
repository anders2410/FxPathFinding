package paths;

import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.BiFunction;

import static org.junit.Assert.*;

public class PathExperiments {
    Graph graph;
    String fileName = "malta-latest.osm.pbf";
    GraphIO graphIO;

    @Before
    public void setUp() {
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
    }

    @Test
    public void landmarksComparisonTest() {
        int testSize = 4000;

        SSSP.setGraph(graph);

        ShortestPathResult[][] resultArray = new ShortestPathResult[4][testSize];

        TestData maxData = new TestData();
        TestData avoidData = new TestData();
        TestData farthestData = new TestData();
        TestData randomData = new TestData();

        long[][] runtimes = new long[4][testSize];
        Landmarks lm = new Landmarks(graph);
        SSSP.setLandmarks(lm);
        lm.landmarksRandom(16, true);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        Set<Integer> test = lm.getLandmarkSet();
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            /*resultArray[0][j] = res;*/
            runtimes[0][j] = res.runTime;
            randomData.addVisit(res.calculateAllUniqueVisits(graph));
            randomData.addRuntime(res.runTime);
        }
        lm.clearLandmarks();
        lm.landmarksFarthest(16, true);
        test = lm.getLandmarkSet();
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            /*resultArray[1][j] = res;*/
            runtimes[1][j] = res.runTime;
            farthestData.addVisit(res.calculateAllUniqueVisits(graph));
            farthestData.addRuntime(res.runTime);
        }
        lm.clearLandmarks();
        lm.landmarksAvoid(16, true);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        test = lm.getLandmarkSet();
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
/*
            resultArray[2][j] = res;
*/
            runtimes[2][j] = res.runTime;
            avoidData.addVisit(res.calculateAllUniqueVisits(graph));
            avoidData.addRuntime(res.runTime);
        }

        lm.clearLandmarks();
        lm.landmarksMaxCover(16, true);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        test = lm.getLandmarkSet();
        SSSP.setLandmarkArray(null);
        for (int j = 0; j < testSize; j++) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
/*
            resultArray[3][j] = res;
*/
            runtimes[3][j] = res.runTime;
            maxData.addVisit(res.calculateAllUniqueVisits(graph));
            maxData.addRuntime(res.runTime);
        }
        long max = 0;
        long random = 0;
        long avoid = 0;
        long farthest = 0;

        for (int i = 0; i < testSize; i++) {
            random += runtimes[0][i];
            farthest += runtimes[1][i];
            avoid += runtimes[2][i];
            max += runtimes[3][i];
        }
        assertTrue(max < random);
    }
}

class TestData {
    protected int minVisits, maxVisits, acumVisits;
    protected long acumRuntime;
    protected List<Integer> pathStartList;

    public TestData() {
        minVisits = Integer.MAX_VALUE;
        maxVisits = 0;
        acumVisits = 0;
        acumRuntime = 0;
        pathStartList = new ArrayList<>();
    }

    public void addVisit(int size) {
        if (size < minVisits && size != 0) minVisits = size;
        if (size > maxVisits) maxVisits = size;
        acumVisits += size;
    }

    public void addRuntime(long runTime) {
        acumRuntime += runTime;
    }

    public void addStartingPoint(Integer integer) {
        pathStartList.add(integer);
    }
}

