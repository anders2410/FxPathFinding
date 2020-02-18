package model;

public class Util {

    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static double flatEarthDistance(Node node1, Node node2) {
        double latDif = Math.abs(node1.latitude - node2.latitude);
        double lonDif = Math.abs(node1.longitude - node2.longitude);
        return Math.sqrt(Math.pow(latDif, 2) + Math.pow(lonDif, 2));
    }

    public static double sphericalDistance(Node node1, Node node2) {
        var earthRadiusKm = 6371;

        var dLat = degreesToRadians(node2.latitude - node1.latitude);
        var dLon = degreesToRadians(node2.longitude - node2.longitude);

        var lat1rad = degreesToRadians(node1.latitude);
        var lat2rad = degreesToRadians(node2.latitude);

        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1rad) * Math.cos(lat2rad);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        assert c >= 0;
        return earthRadiusKm * c;
    }

    public static int cordToInt(String attriVal) {
        String intString = attriVal.replace(".", "");
        return Integer.parseInt(intString);
    }

    public static double cordToDouble(String attriVal) {
        return Double.parseDouble(attriVal);
    }
}
