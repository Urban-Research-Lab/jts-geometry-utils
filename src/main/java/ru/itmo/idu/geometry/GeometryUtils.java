package ru.itmo.idu.geometry;

import lombok.val;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Helper methods for working with JTS Geometry class
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class GeometryUtils {

    private static final Logger log = LoggerFactory.getLogger(GeometryUtils.class);

    public static GeometryFactory geometryFactory = new GeometryFactory();

    public static PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();

    /**
     * Creates a line from 2 given points
     */
    public static LineString makeLine(Coordinate start, Coordinate end) {
        return new LineString(
                new CoordinateArraySequence(
                        new Coordinate[]{start, end}
                ), geometryFactory
        );
    }

    public static LineString makeLine(Coordinate... coordinates) {
        return new LineString(
                new CoordinateArraySequence(
                        coordinates
                ), geometryFactory
        );
    }

    /**
     * Creates a line from a list of x,y points
     */
    public static LineString makeLine(double[][] latLonPoints) {
        Coordinate[] coordinates = new Coordinate[latLonPoints.length];

        for (int i = 0; i < latLonPoints.length; ++i) {
            if (latLonPoints[i].length >= 3) {
                coordinates[i] = new Coordinate(latLonPoints[i][0], latLonPoints[i][1], latLonPoints[i][2]);
            } else {
                coordinates[i] = new Coordinate(latLonPoints[i][0], latLonPoints[i][1]);
            }
        }

        return new LineString(
                new CoordinateArraySequence(coordinates),
                geometryFactory
        );
    }

    public static Point makePoint(double x, double y) {
        return makePoint(new Coordinate(x, y));
    }

    public static Point makePoint(Coordinate coordinate) {
        return new Point(new CoordinateArraySequence(new Coordinate[]{coordinate}), geometryFactory);
    }

    /**
     * Checks if given list of coordinates is a closed ring (last point equals to the first one). Adds copy of first point
     * to the end if necessary.
     */
    public static Coordinate[] closeRing(Coordinate... coordinates) {
        if (coordinates[0].equals2D(coordinates[coordinates.length - 1])) {
            return coordinates;
        }
        Coordinate[] rz = new Coordinate[coordinates.length + 1];
        System.arraycopy(coordinates, 0, rz, 0, coordinates.length);
        rz[coordinates.length] = coordinates[0];
        return rz;
    }

    public static Polygon makePolygon(Coordinate... coordinates) {

        return new Polygon(
                new LinearRing(
                        new CoordinateArraySequence(
                                closeRing(coordinates)
                        ),
                        geometryFactory
                ),
                null,
                geometryFactory
        );
    }



    public static Geometry polygonize(Geometry geometry) {
        List lines = LineStringExtracter.getLines(geometry);
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(lines);
        Collection polys = polygonizer.getPolygons();
        Polygon[] polyArray = GeometryFactory.toPolygonArray(polys);
        return geometry.getFactory().createGeometryCollection(polyArray);
    }

    public static List<Geometry> splitPolygon(Geometry poly, Geometry line) {
        return splitPolygon(poly, Collections.singletonList(line));
    }

    /**
     * Splits polygon into parts by cutting it with given lines
     */
    public static List<Geometry> splitPolygon(Geometry poly, List<Geometry> lines) {
        Geometry nodedLinework = poly.getBoundary();
        for (Geometry line: lines) {
            nodedLinework = nodedLinework.union(line);
        }
        Geometry polys = polygonize(nodedLinework);

        // Only keep polygons which are inside the input
        List<Geometry> output = new ArrayList();
        for (int i = 0; i < polys.getNumGeometries(); i++) {
            Polygon candpoly = (Polygon) polys.getGeometryN(i);
            if (poly.contains(candpoly.getInteriorPoint())) {
                output.add(candpoly);
            }
        }
        return output;
    }

    /**
     * Retrieve boundary of an object as a linestring.
     */
    public static Geometry getBoundary(Geometry area) {
        Geometry boundary = null;
        if (area instanceof Polygon) {
            boundary =((Polygon) area).getExteriorRing();
        } else if (area instanceof MultiPolygon ){
            boundary = ((Polygon)area.getGeometryN(0)).getExteriorRing();
            for (int i = 1; i < area.getNumGeometries(); ++i) {
                boundary = boundary.union((((Polygon)area.getGeometryN(i)).getExteriorRing()));
            }
        } else if (area instanceof GeometryCollection) {
            for (int i = 0; i < area.getNumGeometries(); ++i) {
                Geometry part = area.getGeometryN(i);
                if (!(part instanceof Polygon)) {
                    continue;
                }
                if (boundary == null) {
                    boundary = ((Polygon) part).getExteriorRing();
                } else {
                    boundary = boundary.union((((Polygon)part).getExteriorRing()));
                }
            }
            if (boundary == null) {
                throw new IllegalArgumentException("Border is a geometry collection without any polygons");
            }
        } else {
            throw new IllegalArgumentException("Unknown border shape type: " + area.getClass().getSimpleName());
        }
        return boundary;
    }

    /**
     * Returns all interior rings of a polygon as a geometry collection of line strings
     */
    public static Geometry getAllHoleRings(Polygon polygon) {
        Geometry rz = new GeometryCollection(new Geometry[]{}, geometryFactory);
        for (int i = 0; i < polygon.getNumInteriorRing(); ++i) {
            rz = rz.union(polygon.getInteriorRingN(i));
        }
        return rz;
    }

    /**
     * Same as above, but works for MultiPolygons too
     */
    public static Geometry getAllHoleRings(Geometry area) {
        Geometry rz = new GeometryCollection(new Geometry[]{}, geometryFactory);
        if (area instanceof Polygon) {
            rz = rz.union(getAllHoleRings((Polygon)area));
        } else if (area instanceof MultiPolygon) {
            for (int i = 0; i < area.getNumGeometries(); ++i) {
                rz = rz.union(getAllHoleRings((Polygon)area.getGeometryN(i)));
            }
        }
        return rz;
    }

    /**
     * Attempts to use some techniques that can fix some of topology errors
     * May fail and return null
     */
    public static Geometry fixGeometry(Geometry geometry, TopologyValidationError error) {
        try {
            if (error.getErrorType() == TopologyValidationError.SELF_INTERSECTION
                    || error.getErrorType() == TopologyValidationError.RING_SELF_INTERSECTION) { // self-intersections may be healed by building a buffer
                return geometry.buffer(0.0);
            }
            if (error.getErrorType() == TopologyValidationError.HOLE_OUTSIDE_SHELL && geometry instanceof Polygon) { // instead of building polygon with holes - build a polygon and then subtract holes one by one
                val polygon = (Polygon)geometry;
                val exterior = (LinearRing) polygon.getExteriorRing();
                val holesCount = polygon.getNumInteriorRing();
                var newPolygon = new Polygon(exterior, null, geometryFactory);
                for (int holeIdx = 0; holeIdx < holesCount; ++holeIdx) {
                    val hole = new Polygon((LinearRing)polygon.getInteriorRingN(holeIdx), null, geometryFactory);
                    newPolygon = (Polygon) (newPolygon.difference(hole));
                }
                return newPolygon;
            }
        } catch (TopologyException ex) {
            return null;
        }
        return null;
    }

    public static List<LineSegment> geometrySegmentList(Geometry geometry) {
        List<LineSegment> segmentList = new ArrayList<>();
        if(geometry.getCoordinates().length < 2) {
            return segmentList;
        }

        for(int i = 0; i < geometry.getCoordinates().length -1; ++i) {
            LineSegment segment = new LineSegment(
                    geometry.getCoordinates()[i],
                    geometry.getCoordinates()[i + 1]
            );

            segmentList.add(segment);
        }

        return segmentList;
    }

    public static double angleToAzimuth(double angle) {
        double azimuth = Math.toDegrees((Math.PI / 2) - angle);
        return fixAzimuth(azimuth);
    }

    public static double azimuthToAngle(double azimuth) {
        return Math.toRadians(
                fixAzimuth(90 - azimuth)
        );
    }

    /**
     * If azimuth is negative, it converts the azimuth into positive equivalent between 0 and 360
     * If azimuth is positive and greater than 360, it converts to the equivalent between 0 and 360
     */
    public static double fixAzimuth(double azimuth) {
        return azimuth < 0
                ? azimuth + 360 * Math.ceil(Math.abs(azimuth) / 360)
                : azimuth % 360;
    }

    /**
     * Accepts a LineString consisting of two coordinates and increases its length by a fraction of its length.
     * E.g. when a 0.5 fraction is passed, the line will be prolonged by 25% of its length from each end.
     * */
    public static LineString increaseLineLength(LineString ls, double fraction) {
        Coordinate[] coordinates = ls.getCoordinates();
        if(coordinates.length == 0 || ls.isEmpty()) {
            return ls;
        }

        if(coordinates.length != 2) {
            throw new IllegalArgumentException(
                    String.format("LineString with 2 coordinates expected; %d coordinates provided", coordinates.length)
            );
        }

        LineSegment segment = new LineSegment(coordinates[0], coordinates[1]);
        Coordinate newCoord1 = segment.pointAlong(fraction / 2 * -1);
        Coordinate newCoord2 = segment.pointAlong(fraction / 2 + 1d);

        return makeLine(newCoord1, newCoord2);
    }

    public static Geometry makePolygonFromCoordinates (List<Coordinate> coordinates, double buffer){
        List<Geometry> pointsList = new ArrayList<>();
        coordinates.forEach(sp -> pointsList.add(ProjectionUtils.bufferProjected(GeometryUtils.makePoint(sp), buffer)));
        Geometry[] pointsArray = pointsList.toArray(new Geometry[0]);
        Geometry geometryCollection  = geometryFactory.createGeometryCollection(pointsArray);
        geometryCollection = geometryCollection.union();
        return geometryCollection;
    }

    /**
     * This method works with geometry collections, while default JTS method does not
     * Can be slow
     */
    public static Geometry geometryCollectionDifference(Geometry first, Geometry second) {
        if (first.isEmpty()) {
            return first;
        }
        if (second.isEmpty()) {
            return first;
        }
        List<Geometry> results = new ArrayList<>(first.getNumGeometries());
        for (int geomIdx1 = 0; geomIdx1 < first.getNumGeometries(); ++geomIdx1) {
            Geometry part = first.getGeometryN(geomIdx1);
            for (int geomIdx2 = 0; geomIdx2 < second.getNumGeometries(); ++geomIdx2) {
                part = SafeOperations.safeDifference(part, second.getGeometryN(geomIdx2));
                if (part.isEmpty()) {
                    break;
                }
            }
            if (!part.isEmpty()) {
                results.add(part);
            }
        }
        if (results.isEmpty()) {
            return geometryFactory.createEmpty(first.getDimension());
        }
        if (results.size() == 1) {
            return results.get(0);
        }
        return geometryFactory.createGeometryCollection(results.toArray(new Geometry[results.size()]));
    }
}
