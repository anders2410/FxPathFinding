package pbfparsing;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// A tutorial for the Framework can be found at http://jaryard.com/projects/osm4j/tutorial/index.html
public class PBFParser {

    private Object extractGraph(String filename) throws FileNotFoundException {
        Set<String> validNodes = findValidNodes(filename);
        return buildGraph(filename, validNodes);
    }

    private Object buildGraph(String filename, Set<String> validNodes) throws FileNotFoundException {
        File file = new File(filename);
        FileInputStream input = new FileInputStream(file);
        Iterator<EntityContainer> iterator = new PbfIterator(input, false);
        while (iterator.hasNext()) {
            EntityContainer container = iterator.next();
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                if (validNodes.contains(Long.toString(node.getId()))) {
                    Object graphNode = constructGraphNode(node);
                    addGraphNodeToGraph(graphNode);
                }
            }
            if (container.getType() == EntityType.Way) {
                OsmWay way = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
                String highwayValue = tags.get("highway");
                if (highwayValue == null) {
                    continue;
                }
                boolean filtered = shouldFilter(highwayValue);
                if (filtered) {
                    continue;
                }
                addEdgesGraph(way);
            }
        }
        return null;
    }

    private void addEdgesGraph(OsmWay way) {
        // TODO: Integrate with our EDGE representation
    }

    private void addGraphNodeToGraph(Object graphNode) {
    }

    private Object constructGraphNode(OsmNode node) {
        // TODO: Integrate with our NODE class from main project.

        return null;
    }

    private Set<String> findValidNodes(String filename) throws FileNotFoundException {
        File file = new File(filename);
        FileInputStream input = new FileInputStream(file);
        Iterator<EntityContainer> iterator = new PbfIterator(input, false);
        HashSet<String> nodeSet = new HashSet<>();
        while (iterator.hasNext()) {
            EntityContainer container = iterator.next();
            if (container.getType() == EntityType.Way) {
                OsmWay node = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);
                String highwayValue = tags.get("highway");
                if (highwayValue == null) {
                    continue;
                }
                boolean filtered = shouldFilter(highwayValue);
                if (filtered) {
                    continue;
                }
                int nodeNum = node.getNumberOfNodes();
                for (int i = 0; i < nodeNum; i++) {
                    nodeSet.add(Long.toString(node.getNodeId(i)));
                }

            }
        }
        return nodeSet;
    }

    private boolean shouldFilter(String highwayValue) {
        // TODO: Add more filters from main project.
        return highwayValue.equals("path") || highwayValue.equals("raceway") || highwayValue.equals("cycleway");
    }

    /*private void TestSpeedRaw(String filename) throws FileNotFoundException {
        File file = new File(filename);
        FileInputStream input = new FileInputStream(file);
        Iterator<EntityContainer> iterator = new PbfIterator(input, false);
        long starttime = System.currentTimeMillis();
        while (iterator.hasNext()) {
            EntityContainer container = iterator.next();
        }
        long endtime = System.currentTimeMillis();
        System.out.println("Time to parse denmark.pbf once : " + (endtime - starttime) + " ms");

        *//*File file1 = new File("denmark-latest.osm");
        input = new FileInputStream(file1);
        OsmXmlIterator iterator1 = new OsmXmlIterator(input, false);
        long starttime1 = System.currentTimeMillis();
        while (iterator1.hasNext()) {
            EntityContainer container = iterator1.next();
        }
        long endtime1 = System.currentTimeMillis();
        System.out.println("Time to parse denmark.xml once : " + (endtime1 - starttime1) + " ms");*//*
    }*/
}
