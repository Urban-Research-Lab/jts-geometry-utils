package ru.itmo.idu.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * It is a common problem when noone knows if lat or lon should be on first place.
 * We use a lat-lon coordinates, while geojson and some external tools use lon-lat.
 * So we must inverse coordinates.
 */
public class InvertCoordinateFilter implements CoordinateFilter {
    @SuppressWarnings("SuspiciousNameCombination")

    private Set<Coordinate> inverted = new HashSet<>();

    public void filter(Coordinate coord) {
        if (coord == null) {
            return;
        }
        if (inverted.contains(coord)) {
            // already inverted this instance before, prevent inverting same coordinate twice if they meet several times in single geometry
            return;
        }
        double oldX = coord.x;
        coord.x = coord.y;
        coord.y = oldX;
        inverted.add(coord);
    }
}

