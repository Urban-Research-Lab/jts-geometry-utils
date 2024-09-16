package ru.itmo.idu.geometry.algorithms;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import ru.itmo.idu.geometry.GeometryUtils;

import java.util.ArrayList;
import java.util.List;

public class LineStraightener {

    /**
     * Given a line, area and points, tries to straighten the line (throwing away intermediate points). Straightened
     * line shall be within given area. Provided pointsToKeep are not thrown away
     */
    public static LineString straightenLineString(LineString lineToStraighten,
                                                   Geometry area,
                                                   List<Coordinate> pointsToKeep) {
        Coordinate[] coords = lineToStraighten.getCoordinates();
        if (coords.length <= 2) {
            return lineToStraighten;
        }
        var startIdx = 0;
        var rayIdx = 1;
        var newCoords = new ArrayList<Coordinate>();
        newCoords.add(coords[startIdx]);

        PreparedGeometry preparedArea = area != null ? GeometryUtils.prepareGeometry(area) : null;

        do {
            var startCoord = coords[startIdx];
            var endCoord = coords[rayIdx];
            var line = GeometryUtils.makeLine(startCoord, endCoord);
            if (pointsToKeep != null && pointsToKeep.contains(endCoord)) {
                newCoords.add(coords[rayIdx]);
                startIdx = rayIdx;
                rayIdx = startIdx + 1;
            } else if (preparedArea != null && !preparedArea.contains(line)) {
                if (rayIdx == startIdx + 1) {
                    // weird line with segments not inside area
                    newCoords.add(coords[rayIdx]);
                    startIdx = rayIdx;
                    rayIdx = startIdx + 1;
                } else {
                    newCoords.add(coords[rayIdx - 1]);
                    startIdx = rayIdx - 1;
                }
            } else {
                ++rayIdx;
            }
        } while (rayIdx < coords.length - 1 );

        var lastLine = GeometryUtils.makeLine(coords[startIdx], coords[rayIdx]);
        if (preparedArea != null && !preparedArea.contains(lastLine)) {
            newCoords.add(coords[rayIdx - 1]);
        }
        newCoords.add(coords[rayIdx]);

        return GeometryUtils.makeLine(newCoords.toArray(new Coordinate[0]));
    }


}
