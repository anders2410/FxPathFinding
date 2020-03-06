package model;

import paths.AlgorithmMode;

import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static paths.AlgorithmMode.*;

public class Util {

    public static Map<AlgorithmMode, String> algorithmNames = new HashMap<>();

    static {
        algorithmNames.put(DIJKSTRA, "Dijkstra");
        algorithmNames.put(BI_DIJKSTRA, "Bidirectional Dijkstra");
        algorithmNames.put(A_STAR, "A*");
        algorithmNames.put(BI_A_STAR, "Bidirectional A*");
    }

    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static double flatEarthDistance(Node node1, Node node2) {
        double latDif = node1.latitude - node2.latitude;
        double lonDif = node1.longitude - node2.longitude;
        return Math.sqrt(Math.pow(latDif, 2) + Math.pow(lonDif, 2));
    }

//    public static double sphericalDistance(Node node1, Node node2) {
//        //This function takes in latitude and longitude of two location and returns the distance between them as the crow flies (in km)
//        double R = 6371.0; // km
//        double dLat = degreesToRadians(Math.abs(node2.latitude - node1.latitude));
//        double dLon = degreesToRadians(Math.abs(node2.longitude - node1.longitude));
//        double lat1 = degreesToRadians(node1.latitude);
//        double lat2 = degreesToRadians(node2.latitude);
//        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
//                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        return R * c;
//    }

    public static double sphericalDistance(Node node1, Node node2) {
        // https://rosettacode.org/wiki/Haversine_formula#Java
        //This function takes in latitude and longitude of two location and returns the distance between them as the crow flies (in km)
        double R = 6372.8; // km
        double dLat = Math.toRadians(Math.abs(node2.latitude - node1.latitude));
        double dLon = Math.toRadians(Math.abs(node2.longitude - node1.longitude));
        double lat1 = Math.toRadians(node1.latitude);
        double lat2 = Math.toRadians(node2.latitude);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        assert R * c >= 0;
        return R * c;
    }

    /*public static double sphericalDistance(Node node1, Node node2) {
        // The math module contains a functio named toRadians which converts from degrees to radians.
        double lon1 = Math.toRadians(node1.longitude);
        double lon2 = Math.toRadians(node2.longitude);
        double lat1 = Math.toRadians(node1.latitude);
        double lat2 = Math.toRadians(node1.latitude);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return (c * r);
    }*/

    public static double cordToInt(String attriVal) {
        String intString = attriVal.replace(".", "");
        return Integer.parseInt(intString);
    }

    public static double cordToDouble(String attriVal) {
        return Math.round(Double.parseDouble(attriVal) * 10000000.0) / 10000000.0;
    }

    public static String roundDouble(double value) {
        if (value == Double.MAX_VALUE) {
            String infinitySymbol;
            try {
                return new String(String.valueOf(Character.toString('\u221E')).getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                return infinitySymbol = "NO REACH";
            }
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
/*
        DecimalFormat df = new DecimalFormat("##.#####");
*/
        DecimalFormat df = (DecimalFormat) nf;
        df.setMaximumFractionDigits(4);
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(value);
    }
}
