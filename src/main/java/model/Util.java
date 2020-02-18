package model;

public class Util {
    public static double getNodeDistance(Node node1, Node node2) {
        /*double latDif = Math.abs(node1.latitude - node2.latitude);
        double lonDif = Math.abs(node1.longitude - node2.longitude);
        return Math.sqrt(Math.pow(latDif, 2) + Math.pow(lonDif, 2));*/
        return distanceInKmBetweenEarthCoordinates(node1.latitude, node1.longitude, node2.latitude, node2.longitude);
    }

    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static double distanceInKmBetweenEarthCoordinates(double lat1, double lon1, double lat2, double lon2) {
        var earthRadiusKm = 6371;

        var dLat = degreesToRadians(lat2 - lat1);
        var dLon = degreesToRadians(lon2 - lon1);

        lat1 = degreesToRadians(lat1);
        lat2 = degreesToRadians(lat2);

        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
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
