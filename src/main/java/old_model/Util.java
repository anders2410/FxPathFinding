package old_model;

public class Util {
    public static float getNodeDistance(Node node1, Node node2) {
        return flatEarthDistance(node1, node2);
    }

    private static float flatEarthDistance(Node node1, Node node2) {
        int latDif = node1.latitude - node2.latitude;
        int lonDif = node1.longitude - node2.longitude;
        return (float) Math.sqrt(Math.pow(latDif, 2) + Math.pow(lonDif, 2));
    }

    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static float sphericalDistance(Node node1, Node node2) {
        var earthRadiusKm = 6371;

        var dLat = degreesToRadians(node1.latitude - node2.latitude);
        var dLon = degreesToRadians(node1.longitude - node2.longitude);

        var lat1rad = degreesToRadians(node1.latitude);
        var lat2rad = degreesToRadians(node2.latitude);

        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1rad) * Math.cos(lat2rad);
        float c = (float) (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
        return earthRadiusKm * c;
    }

    public static int cordToInt(String attriVal) {
        String intString = attriVal.replace(".", "");
        return Integer.parseInt(intString);
    }
}