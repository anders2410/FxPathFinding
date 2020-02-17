package model;

public class Util {
    public static float getNodeDistance(Node node1, Node node2) {
        double latDif = node1.latitude - node2.latitude;
        double lonDif = node1.longitude - node2.longitude;
        return (float) Math.sqrt(Math.pow(latDif, 2) + Math.pow(lonDif, 2));
    }

    public static int cordToInt(String attriVal) {
        String intString = attriVal.replace(".", "");
        return Integer.parseInt(intString);
    }
}
