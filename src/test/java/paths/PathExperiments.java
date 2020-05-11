package paths;

import load.GraphIO;
import model.Graph;
import model.Node;
import org.junit.Test;
import paths.preprocessing.LandmarkMode;
import paths.preprocessing.Landmarks;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

import static paths.SSSP.seed;

public class PathExperiments {

    String fileName = "malta-latest.osm.pbf";
    BiFunction<Node, Node, Double> distanceStrategy = Util::sphericalDistance;

    GraphIO graphIO;
    Graph graph;

    private void setUp(String fileName) {
        SSSP.setDistanceStrategy(distanceStrategy);

        graphIO = new GraphIO(distanceStrategy, true);
        assert graphIO.fileExtensionExists(fileName, "-graph.tmp"); // Check that scc exists
        graphIO.loadAllPreProcessing(fileName);
        graph = SSSP.getGraph();
    }

    int testCases = 1000;

    @Test
    public void testCompareAlgorithms() {
        setUp("malta-latest.osm.pbf");
        seed = 0;
        while (seed < testCases) {
            seed++;

            ShortestPathResult res = SSSP.findShortestPath(500, 300, AlgorithmMode.SINGLE_TO_ALL);
            RunningTimeResult testRes = new RunningTimeResult(res.scannedNodesA.size() + res.scannedNodesB.size(), res.runTime);
        }
    }

    @Test
    public void allSpeedTestOne(){
        setUp("malta-latest.osm.pbf");
        Instant start = Instant.now();
        SSSP.findShortestPath(500, 300, AlgorithmMode.SINGLE_TO_ALL);
        Instant end = Instant.now();
        long timeElapsed = Duration.between(start, end).toMillis();
        System.out.println(timeElapsed);
    }

    @Test
    public void compareAllAlgorithmsOnDifferentParameters() {
        setUp(fileName);

        TestDataExtra dijkstraData = new TestDataExtra("Dijkstra");
        TestDataExtra biDijkstraData = new TestDataExtra("Bi-Dijkstra");
        TestDataExtra biAStarData = new TestDataExtra("Bi-AStar");
        TestDataExtra ALTData = new TestDataExtra("BiALT");
        TestDataExtra ReachData = new TestDataExtra("REAL");
        TestDataExtra CHData = new TestDataExtra("CH");

        testAlgorithm(AlgorithmMode.DIJKSTRA, dijkstraData);
        testAlgorithm(AlgorithmMode.BI_DIJKSTRA, biDijkstraData);
        testAlgorithm(AlgorithmMode.BI_A_STAR_CONSISTENT, biAStarData);
        testAlgorithm(AlgorithmMode.BI_A_STAR_LANDMARKS, ALTData);
        testAlgorithm(AlgorithmMode.BI_REACH_LANDMARKS, ReachData);
        testAlgorithm(AlgorithmMode.CONTRACTION_HIERARCHIES, CHData);

        System.out.println(dijkstraData);
        System.out.println(biDijkstraData);
        System.out.println(biAStarData);
        System.out.println(ALTData);
        System.out.println(ReachData);
        System.out.println(CHData);
    }

    private void testAlgorithm(AlgorithmMode mode, TestDataExtra data) {
        int i = 0;
        while (i < 1000) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(mode);
            data.addVisit(res.scannedNodesA.size() + res.scannedNodesB.size(), res.path.size(), res.runTime);
            i++;
        }
    }

    @Test
    public void DijkstraSpeedTest() {
        setUp("malta-latest.osm.pbf");
        int testSize = 1000;
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
        System.out.println(data.averageRuntime /testSize);
    }


    @Test
    public void landmarksComparisonTest() {
        setUp("malta-latest.osm.pbf");
        int testSize = 10000;

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
        System.out.println(maxData.averageVisit);
        System.out.println(avoidData.averageVisit);
        System.out.println(randomData.averageVisit);
        System.out.println(farthestData.averageVisit);
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
}

class TestData {
    protected int minVisits, maxVisits, averageVisit;
    protected long averageRuntime;
    protected List<Integer> pathStartList;

    public TestData() {
        minVisits = Integer.MAX_VALUE;
        maxVisits = 0;
        averageVisit = 0;
        averageRuntime = 0;
        pathStartList = new ArrayList<>();
    }

    public void addVisit(int size) {
        if (size < minVisits && size != 0) minVisits = size;
        if (size > maxVisits) maxVisits = size;
        averageVisit += size;
    }

    public void addRuntime(long runTime) {
        averageRuntime += runTime;
    }

    public void addStartingPoint(Integer integer) {
        pathStartList.add(integer);
    }
}

class TestDataExtra {
    private int maxVisits, totalVisits;
    private long totalRuntime;
    private final List<Integer> pathLengthList;
    private final String name;

    protected TestDataExtra(String name) {
        this.name = name;
        maxVisits = 0;
        totalVisits = 0;
        totalRuntime = 0;
        pathLengthList = new ArrayList<>();
    }

    protected void addVisit(int nodesVisited, int pathLength, long runtime) {
        if (nodesVisited > maxVisits) maxVisits = nodesVisited;
        totalRuntime += runtime;
        totalVisits += nodesVisited;
        pathLengthList.add(pathLength);
    }

    protected Integer getAverageVisited() {
        return totalVisits / pathLengthList.size();
    }

    protected Long getAverageRuntime() {
        return totalRuntime / pathLengthList.size();
    }

    @Override
    public String toString() {
        return name + " average nodes visited: " + getAverageVisited() + ", max visited: " + maxVisits + ", average runtime: " + getAverageRuntime();
    }
}

class RunningTimeResult {
    protected int nodesVisited;
    protected double runningTime;

    public RunningTimeResult(int nodesVisited, double runningTime) {
        this.nodesVisited = nodesVisited;
        this.runningTime = runningTime;
    }

    public int getNodesVisited() {
        return nodesVisited;
    }

    public double getRunningTime() {
        return runningTime;
    }
}

