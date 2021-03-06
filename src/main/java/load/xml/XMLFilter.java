package load.xml;

import java.io.*;
import java.util.*;

import org.xml.sax.*;

import javax.xml.parsers.*;

import org.xml.sax.helpers.DefaultHandler;

import static load.GraphIO.getFolderName;
import static load.GraphIO.mapsDir;

public class XMLFilter extends DefaultHandler {
    private final String inFileType = ".osm";
    private final String outFileType = ".load.xml";
    private final String osmVersion = "0.6";

    private String fileName;

    private int indentLevel;
    private Set<String> validNodes;
    private List<String> tempNodeRefs;
    private List<String> stringList;

    private FileWriter fwriter;


    public XMLFilter(String fileName) {
        this.fileName = fileName;
        //Init util vars
        indentLevel = 0;
        validNodes = new HashSet<>();
        stringList = new LinkedList<>();
        tempNodeRefs = new LinkedList<>();
    }

    public void executeFilter() {
        try {
            fwriter = new FileWriter(getFolderName(fileName) + fileName + outFileType);
            File inputFile = new File( mapsDir + fileName + inFileType);

            DefaultHandler handler = this;
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputFile, handler);

            fwriter.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void startDocument() {
        writeLine("<?load.xml version = \"1.0\" encoding = \"UTF-8\"?>");
        writeLine("<osm version=\"" + osmVersion + "\">");
    }

    private boolean highWayDetected;
    private boolean carFreeWayDetected;
    private boolean nodeDetected;
    private boolean wayDetected;

    private boolean saveElement() {
        return nodeDetected || (wayDetected && highWayDetected && !carFreeWayDetected);
    }

    private void resetElementFlags() {
        highWayDetected = false;
        carFreeWayDetected = false;
        nodeDetected = false;
        wayDetected = false;
    }

    private boolean illegalKeyValPair;

    /**
     * Sets the above flags ^
     * Adds 1 to indent level
     * Updates saveElement to true if element should be kept
     * Adds first line of element to stringList
     * Stores list of tempNodeRef to compute out degrees
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        indentLevel++;
        String processText = indentation() + "<" + qName;

        nodeDetected = nodeDetected || qName.equals("node");
        wayDetected = wayDetected || qName.equals("way");

        if (attributes == null) {
            stringList.add(processText + '>');
            return;
        }

        for (int i = 0; i < attributes.getLength(); i++) {
            String attriKey = attributes.getQName(i);
            String attriVal = attributes.getValue(i);

            if (ifIllegalKey(attriKey)) {
                continue;
            }

            illegalKeyValPair = illegalKeyValPair || ifIllegalVal(attriKey, attriVal);

            // If we have a tag element, we can start filtering away the super node element,
            // because <tag> elements exists only inside of <ways> and <node>
            if (qName.equals("tag")) {
                carFreeWayDetected = carFreeWayDetected || attriKey.equals("v") && illegalRoads(attriVal);
                highWayDetected = highWayDetected || attriKey.equals("k") && attriVal.equals("highway");
            }

            // <nd> is a elemName existing only in <way> elements
            if (qName.equals("nd")) {
                if (attriKey.equals("ref")) {
                    tempNodeRefs.add(attriVal);
                }
            }

            // Handle &amp; &gt;
            attriVal = replaceSpecialCharacters(attriVal);
            processText += ' ' + attriKey + "=\"" + attriVal + '"';
        }
        if (!(illegalKeyValPair || ifIllegalNestedNode(qName))) {
            stringList.add(processText + '>');
        }
    }

    private boolean ifIllegalNestedNode(String qName) {
        return nodeDetected && qName.equals("tag");
    }

    private boolean ifIllegalVal(String attriKey, String attriVal) {
        return attriKey.equals("k") && (attriVal.equals("name"));
    }

    // Method to filter out keys we don't want to use
    private boolean ifIllegalKey(String attriKey) {
        return attriKey.equals("visible") || attriKey.equals("version") || attriKey.equals("changeset")
                || attriKey.equals("timestamp") || attriKey.equals("user") || attriKey.equals("uid");
    }

    // Method to filter out values/roads we do not want to use
    private boolean illegalRoads(String val) {
        return val.equals("cycleway") || val.equals("footway") || val.equals("path") || val.equals("construction")
                || val.equals("proposed") || val.equals("raceway") || val.equals("escape")
                || val.equals("pedestrian") || val.equals("track") || val.equals("service")
                || val.equals("bus_guideway") || val.equals("steps")
                || val.equals("corridor");
    }

    public String replaceSpecialCharacters(String attriVal) {
        attriVal = attriVal.replace("\"", "&quot;");
        attriVal = attriVal.replace("'", "&apos");
        attriVal = attriVal.replace("<", "&lt;");
        attriVal = attriVal.replace(">", "&gt;");
        attriVal = attriVal.replace("&", "&amp;");

        return attriVal;
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (!(illegalKeyValPair || ifIllegalNestedNode(qName))) {
            stringList.add(indentation() + "</" + qName + '>');
        }
        indentLevel--;

        if (indentLevel == 1) {
            if (saveElement()) {
                // Write elements from nodeRefs to set.
                Iterator<String> iterator = tempNodeRefs.iterator();
                while (iterator.hasNext()) {
                    validNodes.add(iterator.next());
                }

                // Writes element to new file
                iterator = stringList.iterator();
                while (iterator.hasNext()) {
                    writeLine(iterator.next());
                }

            }
            resetElementFlags();
            stringList.clear();
            tempNodeRefs.clear();
        }
        illegalKeyValPair = false;
    }

    @Override
    public void endDocument() throws SAXException {
        writeLine("</osm>");
    }

    private void writeLine(String line) {
        try {
            fwriter.write(line + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String indentation() {
        String indentation = "    ";
        return indentation.repeat(indentLevel - 1);
    }

    public Set<String> getValidNodes() {
        return validNodes;
    }
}