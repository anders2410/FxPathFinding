package pbfparsing.delegates;

import pbfparsing.interfaces.FilteringStrategy;

public class StandardFilteringStrategy implements FilteringStrategy {
    @Override
    public boolean shouldFilter(String roadValue) {
        // TODO: Add more filters from main project.
        return roadValue == null || roadValue.equals("cycleway") || roadValue.equals("footway") || roadValue.equals("path")
                || roadValue.equals("construction") || roadValue.equals("proposed") || roadValue.equals("raceway")
                || roadValue.equals("escape") || roadValue.equals("pedestrian") || roadValue.equals("track")
                || roadValue.equals("service") || roadValue.equals("bus_guideway") || roadValue.equals("steps")
                || roadValue.equals("corridor");
    }
}
