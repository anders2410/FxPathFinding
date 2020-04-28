package paths;

import javafx.concurrent.Task;
import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PathExperiments {
    Graph graph;
    String fileName;
    GraphIO graphIO;

    @Before
    public void setUp() {
        fileName = "denmark-latest.osm.pbf";
        BiFunction<Node, Node, Double> distanceStrategy1 = Util::sphericalDistance;
        SSSP.setDistanceStrategy(distanceStrategy1);
        graphIO = new GraphIO(distanceStrategy1);
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
    }

    @Test
    public void sccPoland() {
        graphIO.loadGraph("poland-latest.osm.pbf");
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        GraphUtil gu = new GraphUtil(graph);
        List<Graph> subGraphs = gu.scc().stream().filter(g -> g.getNodeAmount() > 2).collect(Collectors.toList());
        graph = subGraphs.get(0);
        GraphIO graphIO = new GraphIO(Util::sphericalDistance);
        graphIO.storeTMP(Util.trimFileTypes("poland-latest.osm.pbf").concat("-scc"), graph);
        System.out.println("Finished computing SCC");
    }

    @Test
    public void sccDenmark() {
        graphIO.loadGraph("denmark-latest.osm.pbf");
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        GraphUtil gu = new GraphUtil(graph);
        List<Graph> subGraphs = gu.scc().stream().filter(g -> g.getNodeAmount() > 2).collect(Collectors.toList());
        graph = subGraphs.get(0);
        GraphIO graphIO = new GraphIO(Util::sphericalDistance);
        graphIO.storeTMP(Util.trimFileTypes("denmark-latest.osm.pbf").concat("-scc"), graph);
        System.out.println("Finished computing SCC");
    }

    @Test
    public void landmarksComparisonTest() {
        int testSize = 10000;

        SSSP.setGraph(graph);

        TestData maxData = new TestData();
        TestData avoidData = new TestData();
        TestData farthestData = new TestData();
        TestData randomData = new TestData();

        Landmarks lm = new Landmarks(graph);
        initTestParameters(lm, LandmarkMode.RANDOM);
        testGenerationMethod(testSize, randomData);

        initTestParameters(lm, LandmarkMode.FARTHEST);
        testGenerationMethod(testSize, farthestData);

        initTestParameters(lm, LandmarkMode.AVOID);
        testGenerationMethod(testSize, avoidData);

        initTestParameters(lm, LandmarkMode.MAXCOVER);
        testGenerationMethod(testSize, maxData);
        System.out.println(maxData.acumVisits);
        System.out.println(avoidData.acumVisits);
        System.out.println(randomData.acumVisits);
        System.out.println(farthestData.acumVisits);
    }

    private void initTestParameters(Landmarks lm, LandmarkMode maxcover) {
        lm.clearLandmarks();
        GraphIO.loadLandmarks(fileName, maxcover, lm);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
    }

    private void testGenerationMethod(int testSize, TestData data) {
        int j = 0;
        while (j < testSize) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
            /*resultArray[0][j] = res;*/
            if (res.path.size() > 20) {
                if (j % 1000 == 0) {
                    System.out.println("Runtime for case " + j + "(seed = " + SSSP.seed + ") = " + res.runTime);
                }
                data.addVisit(res.calculateAllUniqueVisits(graph));
                data.addRuntime(res.runTime);
                j++;
            }
        }
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

