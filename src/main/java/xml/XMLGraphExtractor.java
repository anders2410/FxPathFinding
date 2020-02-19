package xml;

import model.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class XMLGraphExtractor extends DefaultHandler {

    private final String inFileType = ".xml";
    private final ArrayList<Node> nodeList;

    private String fileName;
    private Set<String> validNodes;
    private Map<String, Node> nodeMap;
    private Graph graph;

    private BiFunction<Node, Node, Double> distanceStrategy = Util::flatEarthDistance;
    private Function<String, Double> parseCordStrategy = Util::cordToDouble;

    public XMLGraphExtractor(String fileName, Set<String> validNodes) {
        this.fileName = fileName;
        this.validNodes = validNodes;
        // TODO: 04-02-2020 Possibly replace nodeMap. Otherwise too much memory consumption
        nodeMap = new HashMap<>();
        nodeList = new ArrayList<>();
        graph = new Graph(validNodes.size());
    }

    public void executeExtractor() {
        try {
            File inputFile = new File(fileName + inFileType);
            DefaultHandler handler = this;
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputFile, handler);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        // graph.setNodeList(new ArrayList<>(nodeMap.values()));
        graph.setNodeList(nodeList);
    }


    int indexCounter = 0;
    String lastNdID = "";

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "node":
                startNode(attributes);
                break;
            case "way":
                startWay();
                break;
            case "nd":
                startNd(attributes);
                break;
        }

    }

    private void startNode(Attributes attributes) {
        if (attributes != null) {
            String id = "";
            double longitude = 0, latitude = 0;
            for (int i = 0; i < attributes.getLength(); i++) {
                String attriKey = attributes.getQName(i);
                String attriVal = attributes.getValue(i);

                switch (attriKey) {
                    case "id":
                        id = attriVal;
                        break;
                    case "lon":
                        longitude = parseCordStrategy.apply(attriVal);
                        break;
                    case "lat":
                        latitude = parseCordStrategy.apply(attriVal);
                        break;
                }
            }
            if (validNodes.contains(id)) {
                Node node = new Node(indexCounter, latitude, longitude);
                nodeMap.put(id, node);
                nodeList.add(node);
                indexCounter++;
            }
        }
    }

    private void startWay() {
        lastNdID = "";
    }

    private void startNd(Attributes attributes) {
        if (attributes != null) {
            String ndID = "";
            for (int i = 0; i < attributes.getLength(); i++) {
                String attriKey = attributes.getQName(i);
                String attriVal = attributes.getValue(i);

                if ("ref".equals(attriKey)) {
                    ndID = attriVal;
                }
            }
            if (lastNdID.equals("")) {
                lastNdID = ndID;
            } else {
                Node node1 = nodeMap.get(ndID);
                Node node2 = nodeMap.get(lastNdID);
                lastNdID = ndID;
                double d = distanceStrategy.apply(node1, node2);
                graph.addEdge(node1, node2, d);
                graph.addEdge(node2, node1, d);
            }
        }
    }

    public Graph getGraph() {
        return graph;
    }

    public void setDistanceStrategy(BiFunction<Node, Node, Double> distanceStrategy) {
        this.distanceStrategy = distanceStrategy;
    }

    public void setParseCordStrategy(Function<String, Double> parseCordStrategy) {
        this.parseCordStrategy = parseCordStrategy;
    }
}
