package ru.itmo.idu.geometry;

import lombok.val;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Helper methods for working with JTS Geometry class
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class GeometryUtils {

    private static final Logger log = LoggerFactory.getLogger(GeometryUtils.class);

    public static GeometryFactory geometryFactory = new GeometryFactory();

    public static PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();

    public static Geometry makeEmpty() {
        return geometryFactory.createEmpty(2);
    }

    public static Polygon makeEmptyPolygon() {
        return makePolygon();
    }

    public static LineString makeEmptyLine() {
        return makeLine();
    }

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
        if (coordinates.length == 0) {
            return GeometryUtils.geometryFactory.createLineString();
        } else if (coordinates.length == 1) {
            return makeLine(coordinates[0], coordinates[0]);
        }
        return new LineString(
                new CoordinateArraySequence(
                        coordinates
                ), geometryFactory
        );
    }

    public static LineString makeLine(List<Coordinate> coordinates) {
        return makeLine(coordinates.toArray(Coordinate[]::new));
    }

    public static LineString makeLine(double x1, double y1, double x2, double y2) {
        return makeLine(new Coordinate(x1, y1), new Coordinate(x2, y2));
    }

    /**
     * Creates a line from a list of x,y points or x, y, z
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

    public static GeometryCollection makeGeometryCollection(Geometry... geometries) {
        return geometryFactory.createGeometryCollection(geometries);
    }

    public static GeometryCollection makeGeometryCollection(Collection<Geometry> geometries) {
        return geometryFactory.createGeometryCollection(geometries.toArray(Geometry[]::new));
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

    /**
     * Checks if given list forms a closed ring (last coordinate equals to the first one). Copies first point to the end
     * of the list if not
     * Returns a copy of the original list.
     * Original list is not modified.
     */
    public static List<Coordinate> closeRing(List<Coordinate> coordinates) {
        List<Coordinate> rz = new ArrayList<>(coordinates);
        if (coordinates.get(0).equals2D(coordinates.get(coordinates.size() - 1))) {
            return rz;
        }
        rz.add(coordinates.get(0));
        return rz;
    }

    public static Polygon makePolygon(Coordinate... coordinates) {
        if (coordinates.length == 0) {
            return geometryFactory.createPolygon();
        }
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

    public static Polygon makePolygon(List<Coordinate> coordinates) {
        return new Polygon(
                makeRing(coordinates),
                null,
                geometryFactory
        );
    }

    public static LinearRing makeRing(List<Coordinate> coordinates) {
        if (coordinates.isEmpty()) {
            return GeometryUtils.geometryFactory.createLinearRing();
        }
        return  new LinearRing(
                new CoordinateArraySequence(
                        closeRing(coordinates).toArray(Coordinate[]::new)
                ),
                geometryFactory
        );
    }

    public static LinearRing makeRing(Coordinate... coordinates) {
        if (coordinates.length == 0) {
            return GeometryUtils.geometryFactory.createLinearRing();
        }
        return  new LinearRing(
                new CoordinateArraySequence(
                        closeRing(coordinates)
                ),
                geometryFactory
        );
    }


    /**
     * Receives a collection of lines (or any other geometries). Extracts all polygons created by intersections of these lines
     */
    @SuppressWarnings("rawtypes")
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
        List<Geometry> output = new ArrayList<>();
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

    public static Geometry fixGeometry(Geometry geometry) {
        IsValidOp isValidOp = new IsValidOp(geometry);
        final TopologyValidationError validationError = isValidOp.getValidationError();
        return fixGeometry(geometry, validationError);
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
                    val hole = new Polygon(polygon.getInteriorRingN(holeIdx), null, geometryFactory);
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

        Geometry normalized = geometry.norm();
        for(int i = 0; i < normalized.getCoordinates().length -1; ++i) {
            LineSegment segment = new LineSegment(
                    normalized.getCoordinates()[i],
                    normalized.getCoordinates()[i + 1]
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
        return geometryFactory.createGeometryCollection(results.toArray(Geometry[]::new));
    }

    /**
     * Turns GeometryCollection of arbitrary depth (having other GCs as members) into a flat list of simple geometries (ones
     * that have getNumGeometries() = 1)
     */
    public static List<Geometry> flattenGeometry(Geometry geom) {
        if (geom == null || geom.isEmpty()) {
            return emptyList();
        }
        List<Geometry> rz = new ArrayList<>(geom.getNumGeometries());
        for (int geomIdx = 0; geomIdx < geom.getNumGeometries(); ++geomIdx) {
            Geometry part = geom.getGeometryN(geomIdx);
            if (part.getNumGeometries() > 1) {
                rz.addAll(flattenGeometry(part));
            } else {
                rz.add(part);
            }
        }
        return rz;
    }

    public static Geometry tryConvertGCToCorrectSubclass(Geometry geometryCollection) {
        if (geometryCollection.getClass() != GeometryCollection.class) {
            return geometryCollection;
        }
        boolean hasOnlyPoints = true;
        boolean hasOnlyLines = true;
        boolean hasOnlyPolygons = true;
        List<Geometry> flattened = flattenGeometry(geometryCollection);
        for (Geometry g : flattened) {
            if (!(g instanceof Point)) {
                hasOnlyPoints = false;
            }
            if (!(g instanceof LineString)) {
                hasOnlyLines = false;
            }
            if (!(g instanceof Polygon)) {
                hasOnlyPolygons = false;
            }
        }
        if (hasOnlyPoints) {
            return geometryFactory.createMultiPoint(flattened.stream().map(it -> (Point)it).toArray(Point[]::new));
        } else if (hasOnlyLines) {
            return geometryFactory.createMultiLineString(flattened.stream().map(it -> (LineString)it).toArray(LineString[]::new));
        } else if (hasOnlyPolygons) {
            return geometryFactory.createMultiPolygon(flattened.stream().map(it -> (Polygon)it).toArray(Polygon[]::new));
        }
        return geometryCollection;
    }

}
