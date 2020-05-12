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

import static paths.AlgorithmMode.*;
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
        List<AlgorithmMode> modesToTest = Arrays.asList(
                DIJKSTRA,
                BI_DIJKSTRA,
                A_STAR,
                BI_A_STAR_CONSISTENT,
                A_STAR_LANDMARKS,
                BI_A_STAR_LANDMARKS);
        List<Map<AlgorithmMode, TestManyRes>> results = testMany(modesToTest, testCases);
        int unidir_victories = results.stream().map(r -> r.get(DIJKSTRA).getNodesScanned() < r.get(BI_DIJKSTRA).getNodesScanned() ? 1 : 0).reduce(Integer::sum).orElse(-1);
        int unidirastar_victories = results.stream().map(r -> r.get(A_STAR).getNodesScanned() < r.get(BI_A_STAR_CONSISTENT).getNodesScanned() ? 1 : 0).reduce(Integer::sum).orElse(-1);
        int unidirlm_victories = results.stream().map(r -> r.get(A_STAR_LANDMARKS).getNodesScanned() < r.get(BI_A_STAR_LANDMARKS).getNodesScanned() ? 1 : 0).reduce(Integer::sum).orElse(-1);
        System.out.println("Dijkstra visited less nodes than BiDijkstra " + unidir_victories + " times.");
        System.out.println("A* visited less nodes than BiA* " + unidirastar_victories + " times.");
        System.out.println("ALT visited less nodes than BiALT " + unidirlm_victories + " times.");
        System.out.println("Done");
    }

    private List<Map<AlgorithmMode, TestManyRes>> testMany(List<AlgorithmMode> modesToTest, int amount) {
        System.out.println("Experiment on " + amount + " cases begun.");
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results = new ArrayList<>();
        while (seed < amount) {
            if (seed % 100 == 0) {
                System.out.println("Ran " + seed + " shortest paths");
            }
            Map<AlgorithmMode, TestManyRes> resMap = new HashMap<>();
            for (AlgorithmMode mode : modesToTest) {
                resMap.put(mode, convertToTestManyRes(SSSP.randomPath(mode)));
            }
            results.add(resMap);
            seed++;
        }
        return results;
    }

    private TestManyRes convertToTestManyRes(ShortestPathResult spRes) {
        return new TestManyRes(spRes.runTime, spRes.scannedNodesA.size() + spRes.scannedNodesB.size());
    }

    @Test
    public void allSpeedTestOne(){
        setUp("malta-latest.osm.pbf");
        Instant start = Instant.now();
        SSSP.findShortestPath(500, 300, SINGLE_TO_ALL);
        Instant end = Instant.now();
        long timeElapsed = Duration.between(start, end).toMillis();
        System.out.println(timeElapsed);
    }

    @Test
    public void compareAllAlgorithmsOnDifferentParameters() {
        setUp("denmark-latest.osm.pbf");

        TestDataExtra dijkstraData = new TestDataExtra("Dijkstra", DIJKSTRA);
        TestDataExtra biDijkstraData = new TestDataExtra("Bi-Dijkstra", BI_DIJKSTRA);
        TestDataExtra biAStarData = new TestDataExtra("Bi-AStar", BI_A_STAR_CONSISTENT);
        TestDataExtra ALTData = new TestDataExtra("BiALT", BI_A_STAR_LANDMARKS);
        TestDataExtra ReachData = new TestDataExtra("REAL", BI_REACH_LANDMARKS);
        TestDataExtra CHData = new TestDataExtra("CH", CONTRACTION_HIERARCHIES);

        List<TestDataExtra> dataList = new ArrayList<>();
        /*dataList.add(dijkstraData);
        dataList.add(biDijkstraData);
        dataList.add(biAStarData);
        dataList.add(ALTData);
        dataList.add(ReachData);
        dataList.add(CHData);*/
        dataList.add(ALTData);

        for (TestDataExtra data : dataList) {
            testAlgorithm(data);
            System.out.println(data);
        }

        /*System.out.println(dijkstraData);
        System.out.println(dijkstraData.calculateValuesInPathLengthRange(0, 50));
        System.out.println(dijkstraData.calculateValuesInPathLengthRange(51, 100));
        System.out.println(dijkstraData.calculateValuesInPathLengthRange(101, 150));
        System.out.println(dijkstraData.calculateValuesInPathLengthRange(151, 200));
        System.out.println(dijkstraData.calculateValuesInPathLengthRange(201, Integer.MAX_VALUE));*/
    }

    private void testAlgorithm(TestDataExtra data) {
        int i = 0;
        while (i < 1000) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(data.getMode());
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
            ShortestPathResult res = SSSP.randomPath(DIJKSTRA);
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
    private int maxVisits;
    private long totalRuntime;
    private final List<Integer> pathLengthList, nodesVisitedList;
    private final String name;
    private final AlgorithmMode mode;

    protected TestDataExtra(String name, AlgorithmMode mode) {
        this.name = name;
        this.mode = mode;
        maxVisits = 0;
        totalRuntime = 0;
        pathLengthList = new ArrayList<>();
        nodesVisitedList = new ArrayList<>();
    }

    protected void addVisit(int nodesVisited, int pathLength, long runtime) {
        if (nodesVisited > maxVisits) maxVisits = nodesVisited;
        totalRuntime += runtime;
        nodesVisitedList.add(nodesVisited);
        pathLengthList.add(pathLength);
    }

    protected Integer getAverageVisited() {
        int total = nodesVisitedList.stream().mapToInt(i -> i).sum();
        return total / pathLengthList.size();
    }

    protected Long getAverageRuntime() {
        return totalRuntime / pathLengthList.size();
    }

    protected String calculateValuesInPathLengthRange(int from, int to) {
        int total = 0;
        int count = 0;
        int max = 0;
        for (int i = 0; i < pathLengthList.size(); i++) {
            Integer pathLength = pathLengthList.get(i);
            if (pathLength >= from && pathLength <= to) {
                if (nodesVisitedList.get(i) > max) {
                    max = nodesVisitedList.get(i);
                }
                total += nodesVisitedList.get(i);
                count++;
            }
        }

        return name +
                " in range: " + from +
                " to " + to +
                "." + " average nodes visited: " +
                (total/count) + ", max visited: " +
                maxVisits;
    }

    @Override
    public String toString() {
        return name + " average nodes visited: " + getAverageVisited() + ", max visited: " + maxVisits + ", average runtime: " + getAverageRuntime();
    }

    public AlgorithmMode getMode() {
        return mode;
    }
}

class TestManyRes {
    private double runTime;
    private int nodesScanned;

    public TestManyRes(double runTime, int nodesScanned) {
        this.runTime = runTime;
        this.nodesScanned = nodesScanned;
    }

    public double getRunTime() {
        return runTime;
    }

    public int getNodesScanned() {
        return nodesScanned;
    }
}