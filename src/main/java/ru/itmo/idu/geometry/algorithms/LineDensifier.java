package ru.itmo.idu.geometry.algorithms;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import ru.itmo.idu.geometry.GeometryUtils;

import java.util.ArrayList;
import java.util.List;

public class LineDensifier {

    /**
     * Inserts intermediate points into line segments, ensuring that there is no segment longer than
     * maxPointDistance parameter
     */
    private static List<Coordinate> densifySegment(Coordinate start, Coordinate end, double maxPointDistance) {
        List<Coordinate> resultCoords = new ArrayList<>();
        double segmentLength = start.distance(end);

        if (segmentLength <= maxPointDistance) {
            resultCoords.add(end);
            return resultCoords;
        }

        int numPoints = (int) Math.ceil(segmentLength / maxPointDistance);
        double segmentFraction = 1.0 / numPoints;

        for (int j = 1; j < numPoints; j++) {
            double fraction = j * segmentFraction;
            double x = start.getX() + fraction * (end.getX() - start.getX());
            double y = start.getY() + fraction * (end.getY() - start.getY());
            resultCoords.add(new Coordinate(x, y));
        }
        resultCoords.add(end);
        return resultCoords;
    }

    public static LineString densifyLineString(LineString line, double maxPointDistance) {
        if (maxPointDistance <= 0) {
            throw new IllegalArgumentException("Max point distance must be positive");
        }
        if (line.isEmpty()) {
            return line;
        }

        Coordinate[] coords = line.getCoordinates();
        if (coords.length < 2) {
            return line;
        }

        List<Coordinate> resultCoords = new ArrayList<>();
        resultCoords.add(coords[0]);

        for (int i = 1; i < coords.length; i++) {
            Coordinate start = coords[i - 1];
            Coordinate end = coords[i];
            resultCoords.addAll(densifySegment(start, end, maxPointDistance));
        }

        return line.getFactory().createLineString(
            resultCoords.toArray(new Coordinate[0]));
    }

    /**
     * Creates a LineString with intermediate points inserted so that no segment is longer than maxPointDistance
     */
    public static LineString densifyLineSegment(LineSegment segment, double maxPointDistance) {
        if (maxPointDistance <= 0) {
            throw new IllegalArgumentException("Max point distance must be positive");
        }
        if (segment.p0.equals(segment.p1)) {
            return GeometryUtils.makeLine(segment.p0);
        }

        List<Coordinate> resultCoords = new ArrayList<>();
        resultCoords.add(segment.p0);
        resultCoords.addAll(densifySegment(segment.p0, segment.p1, maxPointDistance));
        return GeometryUtils.makeLine(resultCoords);
    }
}
