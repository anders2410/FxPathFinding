package paths;

import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Before;
import org.junit.Test;
import paths.preprocessing.ContractionHierarchiesResult;
import paths.preprocessing.LandmarkMode;
import paths.preprocessing.Landmarks;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

public class PathExperiments {

    String fileName = "malta-latest.osm.pbf";
    BiFunction<Node, Node, Double> distanceStrategy = Util::sphericalDistance;

    GraphIO graphIO;
    Graph graph;

    @Before
    public void setUp() {
        fileName = "malta-latest.osm.pbf";
        SSSP.setDistanceStrategy(distanceStrategy);
        loadAllPreProcessing();
    }

    public void loadAllPreProcessing() {
        graphIO = new GraphIO(distanceStrategy, true);

        assert graphIO.fileExtensionExists(fileName, ".tmp");
        graphIO.loadGraph(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);

        Landmarks landmarks = new Landmarks(graph);
        graphIO.loadBestLandmarks(fileName, landmarks);
        SSSP.setLandmarks(landmarks);
        SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);

        List<Double> reachBounds = graphIO.loadReach(fileName);
        SSSP.setReachBounds(reachBounds);

        ContractionHierarchiesResult ch = graphIO.loadContractionHierarchies(fileName);
    }

    /*@Test
    public void sccPoland() {
        graphIO.loadGraph("malta-latest.osm.pbf");
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
        GraphUtil gu = new GraphUtil(graph);
        List<Graph> subGraphs = gu.scc().stream().filter(g -> g.getNodeAmount() > 2).collect(Collectors.toList());
        graph = subGraphs.get(0);
        GraphIO graphIO = new GraphIO(Util::sphericalDistance, true);
        graphIO.storeGraph(Util.trimFileTypes("malta-latest.osm.pbf").concat("-scc"), graph);
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
        GraphIO graphIO = new GraphIO(Util::sphericalDistance, true);
        graphIO.storeGraph(Util.trimFileTypes("denmark-latest.osm.pbf").concat("-scc"), graph);
        System.out.println("Finished computing SCC");
    }*/

    @Test
    public void testCompareAlgorithms() {

    }

    @Test
    public void allSpeedTestOne(){
        SSSP.setGraph(graph);
        Instant start = Instant.now();
        SSSP.findShortestPath(500, 300, AlgorithmMode.SINGLE_TO_ALL);
        Instant end = Instant.now();
        long timeElapsed = Duration.between(start, end).toMillis();
        System.out.println(timeElapsed);
    }

    @Test
    public void DijkstraSpeedTest() {
        int testSize = 10000;
        SSSP.setGraph(graph);
        TestData data = new TestData();
        int j = 0;
        while (j < testSize) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(AlgorithmMode.DIJKSTRA);
            /*resultArray[0][j] = res;*/
            if (res.path.size() > 20) {
                data.addVisit(res.calculateAllUniqueVisits(graph));
                data.addRuntime(res.runTime);
                j++;
            }
        }
        System.out.println(data.acumRuntime/testSize);
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
        new GraphIO(Util::sphericalDistance, true).loadLandmarks(fileName, maxcover, lm);
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

