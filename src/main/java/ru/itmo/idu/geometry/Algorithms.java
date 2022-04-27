package ru.itmo.idu.geometry;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Complicated geometry processing algorithms
 */
public class Algorithms {

    /**
     * Tries to remove spikes that are thinner than given width in meters.
     * Creates a negative buffer (that eliminates geometry parts that are too narrow) then inflates it back.
     * Resulting geometry is an approximation of source one and may contain artifacts and topology issues.
     * @param block Geometry in WGS84
     * @param minWidth Width in meters
     */
    public static Geometry removeThinSpikes(Geometry block, double minWidth) {
        Geometry cropped = ProjectionUtils.bufferProjected(block, - minWidth / 2);
        if (cropped.isEmpty()) {
            return null;
        }
        Geometry bufferedBlock = ProjectionUtils.bufferProjected(
                cropped, minWidth / 2, new BufferParameters(4, BufferParameters.CAP_SQUARE, BufferParameters.JOIN_MITRE, BufferParameters.DEFAULT_MITRE_LIMIT));
        Geometry result = bufferedBlock.intersection(block);
        // its ok to use mercator here for geometry simplification, as we do not need precise results
        Geometry projected = ProjectionUtils.transformToMercator(result);
        projected = TopologyPreservingSimplifier.simplify(projected, 5);
        return ProjectionUtils.transformFromMercator(projected);
    }

    /**
     * Wipes out too narrow angles like /\ and turn them into /-\
     */
    public static Geometry removeNarrowAngles(Geometry polygon, double minWidthMeters) {
        if (!(polygon instanceof Polygon)) {
            return polygon;
        }
        Geometry boundary = GeometryUtils.getBoundary(ProjectionUtils.transformToMercator(polygon));
        Coordinate[] coordinates = boundary.getCoordinates();
        int length = coordinates.length;
        List<Coordinate> resultCoordinates = new ArrayList<>(coordinates.length);

        for (int pointIdx = 0; pointIdx < length - 1; ++pointIdx) {
            int prevIdx = (pointIdx - 1);
            if (prevIdx < 0) prevIdx = length - 2; // -2 because last point equals to first one and we do not want it
            int nextIdx = (pointIdx + 1) % (length - 1);

            Coordinate point = coordinates[pointIdx];
            Coordinate prevPoint = coordinates[prevIdx];
            Coordinate nextPoint = coordinates[nextIdx];

            LineSegment left = new LineSegment(point, prevPoint);
            LineSegment right = new LineSegment(point, nextPoint);

            double angle = Angle.angleBetween(prevPoint, point, nextPoint);
            if (angle < 0.9 * Math.PI / 2) {
                double minWidthCustomized = angle < Math.PI / 4 ? minWidthMeters * 2 : minWidthMeters; // for very narrow angles it is better to cut them more
                // angle is too sharp
                if (prevPoint.distance(nextPoint) < minWidthCustomized) {
                    // this angle is so sharp its ends are closer than min width
                    // completely remove middle vertex from result
                    continue;
                }

                // try to cut off part of an angle in 5 steps
                double lengthStep = Math.min(left.getLength(), right.getLength()) / 5;

                boolean found = false;
                for (int i = 1; i <= 5; ++i) {
                    Coordinate newLeftPoint = left.pointAlong(i * lengthStep / left.getLength());
                    Coordinate newRightPoint = right.pointAlong(i * lengthStep / right.getLength());

                    if (newLeftPoint.distance(newRightPoint) > minWidthCustomized) {
                        resultCoordinates.add(newLeftPoint);
                        resultCoordinates.add(newRightPoint);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // didnt find anything, should not really get here
                    resultCoordinates.add(point);
                }

            } else {
                resultCoordinates.add(point);
            }
        }
        if (resultCoordinates.size() < 3) {
            return GeometryUtils.geometryFactory.createPolygon();
        }

        return ProjectionUtils.transformFromMercator(GeometryUtils.makePolygon(resultCoordinates.toArray(new Coordinate[resultCoordinates.size()])));
    }


    /**
     * Finds a segment of a border of given geometry, that is closest to given point
     */
    public static LineSegment findNearestBorderSegment(Geometry geometry, Point point) {
        LineSegment closest = new LineSegment(geometry.getCoordinates()[geometry.getCoordinates().length - 1], geometry.getCoordinates()[0]);
        double closestDist = closest.distance(point.getCoordinate());
        for (int i = 0; i < geometry.getCoordinates().length - 1; ++i) {
            LineSegment ls = new LineSegment(geometry.getCoordinates()[i], geometry.getCoordinates()[i + 1]);
            double dist = ls.distance(point.getCoordinate());
            if (dist < closestDist) {
                closestDist = dist;
                closest = ls;
            }
        }
        return closest;
    }

    public static List<LineSegment> findLongestBorderSegments(Geometry geometry, int limit) {
        List<LineSegment> result = new ArrayList<>();
        for (int i = 0; i < geometry.getCoordinates().length - 1; ++i) {
            LineSegment ls = new LineSegment(geometry.getCoordinates()[i], geometry.getCoordinates()[i + 1]);
            result.add(ls);
        }
        result.sort(Comparator.comparingDouble(LineSegment::getLength).reversed());
        return result.subList(0, Math.min(result.size(), limit));
    }
}
