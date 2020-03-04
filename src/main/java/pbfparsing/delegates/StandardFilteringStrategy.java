package pbfparsing.delegates;

import pbfparsing.interfaces.FilteringStrategy;

public class StandardFilteringStrategy implements FilteringStrategy {
    @Override
    public boolean shouldFilter(String roadValue) {
        return roadValue == null || roadValue.equals("cycleway") || roadValue.equals("footway") || roadValue.equals("bridleway")
                || roadValue.equals("path") || roadValue.equals("construction") || roadValue.equals("proposed") || roadValue.equals("raceway")
                || roadValue.equals("escape") || roadValue.equals("pedestrian") || roadValue.equals("track")
                || roadValue.equals("service") || roadValue.equals("bus_guideway") || roadValue.equals("steps")
                || roadValue.equals("corridor") || roadValue.equals("bus_stop") || roadValue.equals("crossing")
                || roadValue.equals("elevator") || roadValue.equals("emergency_access_point") || roadValue.equals("give_way")
                || roadValue.equals("milestone") || roadValue.equals("passing_place") || roadValue.equals("platform")
                || roadValue.equals("rest_area") || roadValue.equals("speed_camera") || roadValue.equals("steet_lamp")
                || roadValue.equals("services") || roadValue.equals("stop") || roadValue.equals("traffic_mirror")
                || roadValue.equals("traffic_signals") || roadValue.equals("trailhead") || roadValue.equals("toll_gantry");
    }
}
