package paths;

import javafx.util.Pair;
import load.GraphIO;
import model.Graph;
import model.ModelUtil;
import model.Node;
import org.junit.Test;
import paths.preprocessing.LandmarkMode;
import paths.preprocessing.Landmarks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static paths.AlgorithmMode.*;
import static paths.SSSP.seed;

public class PathExperiments {

    String fileName = "malta-latest.osm.pbf";
    BiFunction<Node, Node, Double> distanceStrategy = Util::sphericalDistance;

    GraphIO graphIO;
    Graph graph;

    private void setUp(String fileName) {
        SSSP.setDistanceStrategy(distanceStrategy);
        this.fileName = fileName;
        graphIO = new GraphIO(distanceStrategy, true);
        assert graphIO.fileExtensionExists(fileName, "-graph.tmp"); // Check that scc exists
        graphIO.loadPreAll(fileName);
        graph = graphIO.getGraph();
        SSSP.setGraph(graph);
    }

    @Test
    public void testPrintAllTests() {
        int testCases = 10000;
        setUp("malta-latest.osm.pbf");
        List<AlgorithmMode> modesToTest = Arrays.asList(
                BI_REACH_LANDMARKS
        );
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results = testMany(modesToTest, testCases);
        results.forEach(r -> printPair(r.get(BI_REACH_LANDMARKS)));
        System.out.println();
        results.forEach(r -> System.out.print(r.get(BI_REACH_LANDMARKS).pathDistance + ", "));
        System.out.println();
    }

    @Test
    public void testFindScanOverlap() {
        int testCases = 10000;
        setUp("malta-latest.osm.pbf");
        List<AlgorithmMode> modesToTest = Arrays.asList(
                BI_A_STAR_SYMMETRIC
        );
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results = testMany(modesToTest, testCases);
        for (AlgorithmMode mode : modesToTest) {
            System.out.println(Util.algorithmNames.get(mode) + " has an average overlap of: " + results.stream().map(r -> r.get(mode).overlap).reduce(Integer::sum).orElse(0) / testCases);
        }
    }

    @Test
    public void landmarkAmountExperiment() {
        setUp("estonia-latest.osm.pbf");

        int landmarksWanted = 1;
        Landmarks lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");

        landmarksWanted = 3;
        lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");

        landmarksWanted = 10;
        lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");

        landmarksWanted = 16;
        lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");

        landmarksWanted = 32;
        lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");

        landmarksWanted = 64 * 2;
        lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");

        landmarksWanted = 64 * 2 * 2;
        lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");

        landmarksWanted = 64 * 2 * 2;
        lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(landmarksWanted, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testSaveGenerationMethod(10000, landmarksWanted, "Estonia");
    }

    private void testSaveGenerationMethod(int testSize, int landmarks, String landName) {
        int j = 0;
        String fileName = landName + landmarks + ".txt";
        File f = new File(System.getProperty("user.dir") + "/src/test/experimentsaves/" + fileName);
        f.getParentFile().mkdirs();
        TestData data = new TestData();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            while (j < testSize) {
                SSSP.seed++;
                long minExecution = Long.MAX_VALUE;
                ShortestPathResult res = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
                ShortestPathResult res2 = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
                ShortestPathResult res3 = SSSP.randomPath(AlgorithmMode.A_STAR_LANDMARKS);
                minExecution = Math.min(minExecution, res.runTime);
                minExecution = Math.min(minExecution, res2.runTime);
                minExecution = Math.min(minExecution, res3.runTime);
                data.addVisit(res.scannedNodesA.size());
                data.addRuntime(minExecution);
                String resultToSave = "(" + res.scannedNodesA.size() + "," + minExecution + "):";
                out.write(resultToSave);
                if (j % 10 == 0 && j != 0) out.newLine();
                j++;
            }
            out.newLine();
            out.write("Average Computation Time: " + data.getAverageRunningTime());
            out.newLine();
            out.write("Average Visits: " + data.getAverageVisits());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testOutDegrees() {
        List<String> fileNames = Arrays.asList("faroe-islands", "montenegro", "malta", "estonia", "georgia");
        for (String name : fileNames) {
            name = name + "-latest";
            graphIO = new GraphIO(true);
            if (!graphIO.fileExtensionExists(name, "-graph.tmp")) {
                System.out.println(name + " doesn't exist");
                fail();
            }
            graphIO.loadGraph(name);
            ModelUtil modelUtil = new ModelUtil(graphIO.getGraph());
            System.out.println(name + ": " + modelUtil.averageOutDegree());
        }
    }

    @Test
    public void testCorrelationBetweenRuntimeAndScans() {
        int testCases = 10000;
        setUp("faroe-islands-latest.osm.pbf");
        List<AlgorithmMode> modesToTest = Arrays.asList(
                DIJKSTRA,
                BI_DIJKSTRA,
                A_STAR,
                BI_A_STAR_CONSISTENT,
                A_STAR_LANDMARKS,
                BI_A_STAR_LANDMARKS,
                REACH,
                BI_REACH,
                REACH_A_STAR,
                BI_REACH_A_STAR,
                REACH_LANDMARKS,
                BI_REACH_LANDMARKS,
                CONTRACTION_HIERARCHIES
        );
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results = testManyMinTime(modesToTest, testCases);

        for (AlgorithmMode mode : modesToTest) {
            System.out.print(Util.algorithmNames.get(mode) + ": ");
            results.forEach(r -> System.out.print(r.get(mode).nodesScanned + "," + r.get(mode).runTime + ":"));
            System.out.println();
        }

    }

    @Test
    public void testPrintListUniWins() {
        int testCases = 10000;
        setUp("malta-latest.osm.pbf");
        List<AlgorithmMode> modesToTest = Arrays.asList(
                DIJKSTRA,
                BI_DIJKSTRA
        );
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results = testMany(modesToTest, testCases);
        List<Map<AlgorithmMode, TestManyRes>> ud_dijkstra_wins = results.stream().filter(r -> r.get(DIJKSTRA).nodesScanned < r.get(BI_DIJKSTRA).nodesScanned).collect(Collectors.toList());
        ud_dijkstra_wins.forEach(r -> printPair(r.get(DIJKSTRA)));
    }

    @Test
    public void testCompareUniAndBiDir() {
        int testCases = 10000;
        setUp("malta-latest.osm.pbf");
        List<AlgorithmMode> modesToTest = Arrays.asList(
                DIJKSTRA,
                BI_DIJKSTRA,
                A_STAR,
                BI_A_STAR_CONSISTENT,
                A_STAR_LANDMARKS,
                BI_A_STAR_LANDMARKS,
                REACH,
                BI_REACH,
                REACH_A_STAR,
                BI_REACH_A_STAR,
                REACH_LANDMARKS,
                BI_REACH_LANDMARKS
        );
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results = testMany(modesToTest, testCases);
        checkResultsValid(results);

        List<Map<AlgorithmMode, TestManyRes>> ud_dijkstra_wins = results.stream().filter(r -> r.get(DIJKSTRA).nodesScanned < r.get(BI_DIJKSTRA).nodesScanned).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> ud_astar_wins = results.stream().filter(r -> r.get(A_STAR).nodesScanned < r.get(BI_A_STAR_CONSISTENT).nodesScanned).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> ud_lm_wins = results.stream().filter(r -> r.get(A_STAR_LANDMARKS).nodesScanned < r.get(BI_A_STAR_LANDMARKS).nodesScanned).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> ud_re_wins = results.stream().filter(r -> r.get(REACH).nodesScanned < r.get(BI_REACH).nodesScanned).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> ud_rea_wins = results.stream().filter(r -> r.get(REACH_A_STAR).nodesScanned < r.get(BI_REACH_A_STAR).nodesScanned).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> ud_real_wins = results.stream().filter(r -> r.get(REACH_LANDMARKS).nodesScanned < r.get(BI_REACH_LANDMARKS).nodesScanned).collect(Collectors.toList());
        System.out.println("Amount of unidirectional victories");
        System.out.println("Dijkstra visited less nodes than BiDijkstra " + ud_dijkstra_wins.size() + " times.");
        ud_dijkstra_wins.forEach(r -> printPair(r.get(DIJKSTRA)));
        System.out.println();
        System.out.println("A* visited less nodes than BiA* " + ud_astar_wins.size() + " times.");
        ud_astar_wins.forEach(r -> printPair(r.get(DIJKSTRA)));
        System.out.println();
        System.out.println("ALT visited less nodes than BiALT " + ud_lm_wins.size() + " times.");
        ud_lm_wins.forEach(r -> printPair(r.get(DIJKSTRA)));
        System.out.println();
        System.out.println("RE visited less nodes than BiRE " + ud_re_wins.size() + " times.");
        ud_re_wins.forEach(r -> printPair(r.get(DIJKSTRA)));
        System.out.println();
        System.out.println("REA* visited less nodes than BiREA* " + ud_rea_wins.size() + " times.");
        ud_rea_wins.forEach(r -> printPair(r.get(DIJKSTRA)));
        System.out.println();
        System.out.println("REAL visited less nodes than BiREAL " + ud_real_wins.size() + " times.");
        ud_real_wins.forEach(r -> printPair(r.get(DIJKSTRA)));
        System.out.println();

        List<Map<AlgorithmMode, TestManyRes>> dijkstra_astar_overlap = ud_dijkstra_wins.stream().filter(ud_astar_wins::contains).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> dijkstra_lm_overlap = ud_dijkstra_wins.stream().filter(ud_lm_wins::contains).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> astar_lm_overlap = ud_astar_wins.stream().filter(ud_lm_wins::contains).collect(Collectors.toList());
        System.out.println("Intersections");
        System.out.println("Dijkstra better than BiDijkstra in " + dijkstra_astar_overlap.size() + " cases, where A* better than BiA*");
        System.out.println("Dijkstra better than BiDijkstra in " + dijkstra_lm_overlap.size() + " cases, where ALT better than BiALT");
        System.out.println("A* better than BiA* in " + astar_lm_overlap.size() + " cases, where ALT better than BiALT");
        System.out.println("Done");
    }

    @Test
    public void testGenReachDensity() {
        setUp("malta-latest.osm.pbf");
        ModelUtil modelUtil = new ModelUtil(graph);
        List<Integer> reachDensities = modelUtil.computeDensityMeasuresReach(3, SSSP.getReachBounds());
        GraphIO graphIO = new GraphIO(true);
        graphIO.storeDensitiesReach(fileName, reachDensities);
    }

    @Test
    public void testFlipUniSearch() {
        setUp("denmark-latest.osm.pbf");
        SSSP.setDensityMeasures(new GraphIO(true).loadDensitiesReach(fileName));
        List<AlgorithmMode> modesToTest = Arrays.asList(REACH_LANDMARKS);
        int testCases = 1000;
        System.out.println("Experiment on " + testCases + " cases begun.");

        Map<AlgorithmMode, Pair<List<TestManyRes>, List<TestManyRes>>> resMap = new HashMap<>();
        for (AlgorithmMode mode : modesToTest) {
            resMap.put(mode, new Pair<>(new ArrayList<>(), new ArrayList<>()));
        }

        for (int i = 1; i < testCases + 1; i++) {
            if (i % 100 == 0) {
                System.out.println("Ran " + i + " shortest paths");
            }
            for (AlgorithmMode mode : modesToTest) {
                Pair<List<TestManyRes>, List<TestManyRes>> individualPair = resMap.get(mode);
                SSSP.allowFlip = false;
                individualPair.getKey().add(convertToTestManyRes(SSSP.randomPath(mode)));
                SSSP.allowFlip = true;
                individualPair.getValue().add(convertToTestManyRes(SSSP.randomPath(mode)));
                SSSP.allowFlip = false;
            }
            seed++;
        }

        final Map<AlgorithmMode, Pair<Integer, Integer>> avg_pair_result = new HashMap<>();
        resMap.forEach((m, p) -> {
            Integer avg_no_flip = p.getKey().stream().map(r -> r.nodesScanned).reduce(Integer::sum).orElse(0) / testCases;
            Integer avg_flip = p.getValue().stream().map(r -> r.nodesScanned).reduce(Integer::sum).orElse(0) / testCases;
            avg_pair_result.put(m, new Pair<>(avg_no_flip, avg_flip));

            System.out.println(Util.algorithmNames.get(m));
            System.out.println("Without flip an average amount of " + avg_no_flip + " nodes were scanned. The following were better without flip:");
            int counter = 0;
            List<TestManyRes> key = p.getKey();
            for (int i = 0; i < key.size(); i++) {
                TestManyRes resNoFlip = key.get(i);
                TestManyRes resFlip = p.getValue().get(i);
                if (resNoFlip.nodesScanned < resFlip.nodesScanned) {
                    counter++;
                    printPair(resNoFlip);
                }
            }
            System.out.println("\nWith flip an average amount of " + avg_flip + " nodes were scanned. The following were better with flip:");
            for (int i = 0; i < key.size(); i++) {
                TestManyRes resNoFlip = key.get(i);
                TestManyRes resFlip = p.getValue().get(i);
                if (resNoFlip.nodesScanned > resFlip.nodesScanned) {
                    counter++;
                    printPair(resNoFlip);
                }
            }
            System.out.println();
        });
    }

    @Test
    public void testBiDirDensity() {
        int testCases = 1000;
        setUp("denmark-latest.osm.pbf");
        List<AlgorithmMode> modesToTest = Arrays.asList(
                BI_DIJKSTRA_DENSITY
        );
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results = testMany(modesToTest, testCases);
        /*List<Map<AlgorithmMode, TestManyRes>> density_wins = results.stream().filter(r -> r.get(BI_DIJKSTRA_DENSITY).nodesScanned < r.get(BI_DIJKSTRA).nodesScanned).collect(Collectors.toList());
        List<Map<AlgorithmMode, TestManyRes>> amount_wins = results.stream().filter(r -> r.get(BI_DIJKSTRA).nodesScanned < r.get(BI_DIJKSTRA_DENSITY).nodesScanned).collect(Collectors.toList());
        double avg_dens = (double) results.stream().map(r -> r.get(BI_DIJKSTRA_DENSITY).nodesScanned).reduce(Integer::sum).orElse(0) / testCases;
        System.out.println("Bidijkstra with density and amount alternation won " + (float) 100*density_wins.size()/testCases + "%");
        System.out.println("and scanned " + avg_dens + " nodes on average.");
        density_wins.forEach(r -> printPair(r.get(BI_DIJKSTRA)));
        System.out.println();
        double avg_amount = (double) results.stream().map(r -> r.get(BI_DIJKSTRA).nodesScanned).reduce(Integer::sum).orElse(0) / testCases;
        System.out.println("Bidijkstra with amount alternation won " + (float) 100*amount_wins.size()/testCases + "%");
        System.out.println("and scanned " + avg_amount + " nodes on average.");
        amount_wins.forEach(r -> printPair(r.get(BI_DIJKSTRA)));
        System.out.println();*/
        double avg_dens_alt = (double) results.stream().map(r -> r.get(BI_DIJKSTRA_DENSITY).nodesScanned).reduce(Integer::sum).orElse(0) / testCases;
        System.out.println("BiDijk with density and amount alternation scanned " + avg_dens_alt + " nodes on average.");
    }

    private void printPair(TestManyRes res) {
        System.out.print("(" + res.from + ", " + res.to + ") ");
    }

    private void checkResultsValid(List<Map<AlgorithmMode, TestManyRes>> results) {
        for (int i = 0; i < results.size(); i++) {
            Map<AlgorithmMode, TestManyRes> result = results.get(i);
            for (AlgorithmMode mode : result.keySet()) {
                if (result.get(DIJKSTRA).from != result.get(mode).from || result.get(DIJKSTRA).to != result.get(mode).to) {
                    printPair(result.get(DIJKSTRA));
                    printPair(result.get(mode));
                    System.out.println(Util.algorithmNames.get(mode));
                    fail();
                }
            }
        }
    }

    private List<Map<AlgorithmMode, TestManyRes>> testManyMinTime(List<AlgorithmMode> modesToTest, int amount) {
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results1 = testMany(modesToTest, amount);
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results2 = testMany(modesToTest, amount);
        seed = 0;
        List<Map<AlgorithmMode, TestManyRes>> results3 = testMany(modesToTest, amount);

        for (int i = 0; i < results1.size(); i++) {
            Map<AlgorithmMode, TestManyRes> result1 = results1.get(i);
            Map<AlgorithmMode, TestManyRes> result2 = results2.get(i);
            Map<AlgorithmMode, TestManyRes> result3 = results3.get(i);
            for (AlgorithmMode mode : modesToTest) {
                assert result1.get(mode).from == result2.get(mode).from;
                assert result2.get(mode).from == result3.get(mode).from;
                assert result1.get(mode).to == result2.get(mode).to;
                assert result2.get(mode).to == result3.get(mode).to;
                double runTime1 = result1.get(mode).runTime;
                double runTime2 = result2.get(mode).runTime;
                double runTime3 = result3.get(mode).runTime;
                result1.get(mode).runTime = Math.min(runTime1, Math.min(runTime2, runTime3));
                assert result1.get(mode).runTime <= runTime1;
            }
        }
        return results1;
    }

    private List<Map<AlgorithmMode, TestManyRes>> testMany(List<AlgorithmMode> modesToTest, int amount) {
        System.out.println("Experiment on " + amount + " cases begun.");
        List<Map<AlgorithmMode, TestManyRes>> results = new ArrayList<>();
        for (int i = 1; i < amount + 1; i++) {
            if (i % 100 == 0) {
                System.out.println("Ran " + i + " shortest paths");
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

    @Test
    public void testCompareDensityRadius() {
        int testCases = 1000;
        double densityStep = 0.5, densityLower = 8, densityUpper = 12;
        setUp("estonia-latest.osm.pbf");
        List<List<Integer>> densityMeasures = new ArrayList<>();
        for (double d = densityLower; d < densityUpper; d += densityStep) {
            densityMeasures.add(new ModelUtil(graph).computeDensityMeasures(d));
            System.out.println("Computed density measures for radius " + d);
        }

        System.out.println("Experiment on " + testCases + " cases begun.");
        List<List<TestManyRes>> results = new ArrayList<>();
        for (int i = 1; i < testCases + 1; i++) {
            if (i % 100 == 0) {
                System.out.println("Ran " + i + " shortest paths");
            }
            List<TestManyRes> resList = new ArrayList<>();
            for (List<Integer> measures : densityMeasures) {
                SSSP.setDensityMeasures(measures);
                resList.add(convertToTestManyRes(SSSP.randomPath(BI_DIJKSTRA_DENSITY)));
            }
            results.add(resList);
            seed++;
        }

        System.out.println("With a radius of x an average of y nodes were scanned.");

        for (int i = 0; i < densityMeasures.size(); i++) {
            int finalI = i;
            double avg_scans = ((double) results.stream().map(r -> r.get(finalI).nodesScanned).reduce(Integer::sum).orElse(0)) / testCases;
            System.out.print(((i + 1) * densityStep + densityLower) + "," + avg_scans + ":");
        }
        System.out.println();
    }

    private TestManyRes convertToTestManyRes(ShortestPathResult spRes) {
        if (spRes.path.size() == 0) {
            return new TestManyRes(0, 0);
        }
        int overlap = 0;
        for (Integer integer : spRes.scannedNodesA) {
            if (spRes.scannedNodesB.contains(integer)) {
                overlap++;
            }
        }
        return new TestManyRes(spRes.runTime, spRes.d, spRes.scannedNodesA.size() + spRes.scannedNodesB.size(), spRes.path.size(), overlap, spRes.path.get(0), spRes.path.get(spRes.path.size() - 1));
    }

    @Test
    public void allSpeedTestOne() {
        setUp("malta-latest.osm.pbf");
        Instant start = Instant.now();
        SSSP.findShortestPath(500, 300, SINGLE_TO_ALL);
        Instant end = Instant.now();
        long timeElapsed = Duration.between(start, end).toMillis();
        System.out.println(timeElapsed);
    }

    @Test
    public void compareSelectedAlgorithmsOnNodesVisitedAndSpeed() {
        setUp("denmark-latest.osm.pbf");

        Pair<String, AlgorithmMode> dijkstraPair = new Pair<>("Dijkstra", DIJKSTRA);
        Pair<String, AlgorithmMode> biDijkstraPair = new Pair<>("Bi-Dijkstra", BI_DIJKSTRA);
        Pair<String, AlgorithmMode> biAStarPair = new Pair<>("Bi-AStar", BI_A_STAR_CONSISTENT);
        Pair<String, AlgorithmMode> ALTPair = new Pair<>("BiALT", BI_A_STAR_LANDMARKS);
        Pair<String, AlgorithmMode> ReachPair = new Pair<>("REAL", BI_REACH_LANDMARKS);
        Pair<String, AlgorithmMode> CHPair = new Pair<>("CH", CONTRACTION_HIERARCHIES);

        List<Pair<String, AlgorithmMode>> pairList = new ArrayList<>();
//        pairList.add(dijkstraPair);
//        pairList.add(biDijkstraPair);
//        pairList.add(biAStarPair);
//        pairList.add(ALTPair);
//        pairList.add(ReachPair);
        pairList.add(CHPair);

        for (Pair<String, AlgorithmMode> pair : pairList) {
            TestDataExtra data = new TestDataExtra(pair.getKey(), pair.getValue());
            testAlgorithm(data);
            System.out.println(data);
            // printInSections(data, 0, 50, 100, 150, 200);
            // printInSections(data, 0, 125, 250, 375, 500);
        }
    }

    @Test
    public void compareSelectedAlgorithmsOnNodesVisitedAndSpeedREACH() {
        setUp("denmark-latest.osm.pbf");

        Pair<String, AlgorithmMode> dijkstraPair = new Pair<>("Bi-Reach", BI_REACH);
        Pair<String, AlgorithmMode> biDijkstraPair = new Pair<>("Bi-Reach-AStar", BI_REACH_A_STAR);
        Pair<String, AlgorithmMode> biAStarPair = new Pair<>("Bi-REAL", BI_REACH_LANDMARKS);

        List<Pair<String, AlgorithmMode>> pairList = new ArrayList<>();
        pairList.add(dijkstraPair);
        pairList.add(biDijkstraPair);
        pairList.add(biAStarPair);

        for (Pair<String, AlgorithmMode> pair : pairList) {
            TestDataExtra data = new TestDataExtra(pair.getKey(), pair.getValue());
            testAlgorithm(data);
            System.out.println(data);
            // printInSections(data, 0, 50, 100, 150, 200);
            // printInSections(data, 0, 125, 250, 375, 500);
        }
    }

    private void testAlgorithm(TestDataExtra data) {
        int testCases = 10000;
        int i = 0;
        seed = 0;
        while (i < testCases) {
            if (i % 500 == 0) {
                System.out.println("Running test nr: " + i);
            }
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(data.getMode());
            data.addVisit(res);
            i++;
        }
    }

    public void printInSections(TestDataExtra data, int initial, int second, int third, int fourth, int fifth) {
        System.out.println(data.calculateValuesInPathKMRange(initial, second));
        System.out.println(data.calculateValuesInPathKMRange(second, third));
        System.out.println(data.calculateValuesInPathKMRange(third, fourth));
        System.out.println(data.calculateValuesInPathKMRange(fourth, fifth));
        System.out.println(data.calculateValuesInPathKMRange(fifth, Integer.MAX_VALUE - 1));
    }

    @Test
    public void DijkstraSpeedTest() {
        setUp("denmark-latest.osm.pbf");
        int testSize = 100;
        TestData data = new TestData();
        int j = 0;
        while (j < testSize) {
            SSSP.seed++;
            ShortestPathResult res = SSSP.randomPath(DIJKSTRA);
            /*resultArray[0][j] = res;*/
            if (res.path.size() > 20) {
                data.addVisit(res.scannedNodesA.size());
                data.addRuntime(res.runTime);
                j++;
            }
        }
        System.out.println(data.totalRunningTime / testSize);
    }


    @Test
    public void landmarksComparisonTest() {
        setUp("malta-latest.osm.pbf");
        SSSP.setLandmarkArray(null);
        int testSize = 1000;

        TestData maxData = new TestData();
        TestData avoidData = new TestData();
        TestData farthestData = new TestData();
        TestData randomData = new TestData();

        Landmarks lm = new Landmarks(graph);
        lm.clearLandmarks();
        //initTestParameters(lm, LandmarkMode.RANDOM);
        lm.landmarksRandom(300, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testGenerationMethod(testSize, randomData);

        //initTestParameters(lm, LandmarkMode.FARTHEST);
        lm.clearLandmarks();

        lm.landmarksFarthest(300, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testGenerationMethod(testSize, farthestData);

        // initTestParameters(lm, LandmarkMode.AVOID);
        lm.clearLandmarks();

        lm.landmarksAvoid(16, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testGenerationMethod(testSize, avoidData);

        //initTestParameters(lm, LandmarkMode.MAXCOVER);
        lm.clearLandmarks();
        lm.landmarksMaxCover(1, false);
        SSSP.setLandmarks(lm);
        SSSP.seed = 0;
        SSSP.setLandmarkArray(null);
        testGenerationMethod(testSize, maxData);
        System.out.println(maxData.getAverageRunningTime());
        System.out.println(avoidData.getAverageRunningTime());
        System.out.println(randomData.getAverageRunningTime());
        System.out.println(farthestData.getAverageRunningTime());

        System.out.println(maxData.getAverageVisits());
        System.out.println(avoidData.getAverageVisits());
        System.out.println(randomData.getAverageVisits());
        System.out.println(farthestData.getAverageVisits());
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
                data.addVisit(res.scannedNodesA.size());
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
    protected int minVisits, maxVisits, visits;
    protected long totalRunningTime;
    protected List<Integer> pathStartList;
    private int runs;

    public TestData() {
        minVisits = Integer.MAX_VALUE;
        runs = 0;
        maxVisits = 0;
        visits = 0;
        totalRunningTime = 0;
        pathStartList = new ArrayList<>();
    }

    public void addVisit(int size) {
        if (size < minVisits && size != 0) minVisits = size;
        if (size > maxVisits) maxVisits = size;
        runs++;
        visits += size;
    }

    public int getAverageVisits() {
        return visits / runs;
    }

    public long getAverageRunningTime() {
        return totalRunningTime / runs;
    }

    public void addRuntime(long runTime) {
        totalRunningTime += runTime;
    }

    public void addStartingPoint(Integer integer) {
        pathStartList.add(integer);
    }
}

class TestDataExtra {
    private int maxVisits;
    private long totalRuntime;
    private final List<Integer> pathLengthNodesList, nodesVisitedList;
    private final List<Double> pathLengthKMList;
    private final String name;
    private final AlgorithmMode mode;

    protected TestDataExtra(String name, AlgorithmMode mode) {
        this.name = name;
        this.mode = mode;
        maxVisits = 0;
        totalRuntime = 0;
        pathLengthNodesList = new ArrayList<>();
        nodesVisitedList = new ArrayList<>();
        pathLengthKMList = new ArrayList<>();
    }

    protected void addVisit(ShortestPathResult res) {
        int nodesVisited = res.scannedNodesA.size() + res.scannedNodesB.size();
        if (nodesVisited > maxVisits) maxVisits = nodesVisited;
        totalRuntime += res.runTime;
        nodesVisitedList.add(nodesVisited);
        pathLengthNodesList.add(res.path.size());
        pathLengthKMList.add(res.d);
    }

    protected Long getAverageVisited() {
        long total = nodesVisitedList.stream().mapToLong(i -> i).sum();
        return total / nodesVisitedList.size();
    }

    protected Long getAverageRuntime() {
        return totalRuntime / pathLengthNodesList.size();
    }

    /*protected String calculateValuesInPathSizeRange(int from, int to) {
        long total = 0;
        long count = 0;
        long max = 0;
        for (int i = 0; i < pathLengthNodesList.size(); i++) {
            Integer pathLength = pathLengthNodesList.get(i);
            if (pathLength >= from && pathLength < to + 1) {
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
                (total / count) + ", max visited: " +
                max;
    }*/

    protected String calculateValuesInPathKMRange(int from, int to) {
        long total = 0;
        long count = 0;
        long max = 0;
        for (int i = 0; i < pathLengthKMList.size(); i++) {
            Double pathLengthInKM = pathLengthKMList.get(i);
            if (from <= pathLengthInKM && pathLengthInKM < to) {
                if (nodesVisitedList.get(i) > max) {
                    max = nodesVisitedList.get(i);
                }
                total += nodesVisitedList.get(i);
                count++;
            }
        }
        System.out.println(count);
        return name +
                " in range: " + from +
                " to " + to +
                "." + " average nodes visited: " +
                (total / count) + ", max visited: " +
                max;
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
    double runTime;
    double pathDistance;
    int nodesScanned, nodesInPath, overlap;
    int from, to;

    public TestManyRes(double runTime, double pathDistance, int nodesScanned, int nodesInPath, int overlap, int from, int to) {
        this.runTime = runTime;
        this.pathDistance = pathDistance;
        this.nodesScanned = nodesScanned;
        this.nodesInPath = nodesInPath;
        this.overlap = overlap;
        this.from = from;
        this.to = to;
    }

    public TestManyRes(double runTime, int nodesScanned, int from, int to) {
        this.runTime = runTime;
        this.nodesScanned = nodesScanned;
        this.from = from;
        this.to = to;
    }

    public TestManyRes(double runTime, int nodesScanned) {
        this.runTime = runTime;
        this.nodesScanned = nodesScanned;
    }
}