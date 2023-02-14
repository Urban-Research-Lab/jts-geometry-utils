package ru.itmo.idu.geometry;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import static ru.itmo.idu.geometry.CRSUtils.getLocalCRS;
import static ru.itmo.idu.geometry.CRSUtils.getLocalCRSTransform;
import static ru.itmo.idu.geometry.GeometryUtils.makePoint;

/**
 * Methods that need to operate on a projected geometry. E.g. in order to calculate geometry area in meters you need
 * to project it from WGS84 to some plain CRS (e.g. Mercator)
 * All methods accept geometry in WGS84 CRS.
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public class ProjectionUtils {

    public static MathTransform latLonToXY;

    public static MathTransform xyToLatLon;

    static {
        String code = "EPSG:3857";
        CoordinateReferenceSystem auto;
        try {

            auto = CRS.decode(code);
            latLonToXY = CRS.findMathTransform(
                    DefaultGeographicCRS.WGS84, auto);
            xyToLatLon = CRS.findMathTransform(auto,
                    DefaultGeographicCRS.WGS84);
        } catch (FactoryException e) {
            log.error("Failed to create transforms", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * Builds a correct buffer around a point on globe.
     * Unlike usual buffer operation (that works in lat-lon coordinates) it correctly processes difference in lat and long degree length
     * on different locations.
     */
    public static Geometry bufferProjected(Geometry geom,
                                           double meters) {
        return bufferProjected(geom, meters, new BufferParameters(4, BufferParameters.CAP_ROUND, BufferParameters.JOIN_ROUND, BufferParameters.DEFAULT_MITRE_LIMIT));

    }

    public static Geometry bufferProjected(CoordinateReferenceSystem localCrs,
                                           Geometry geom,
                                           double meters) {
        return bufferProjected(localCrs, geom, meters, new BufferParameters(4, BufferParameters.CAP_ROUND, BufferParameters.JOIN_ROUND, BufferParameters.DEFAULT_MITRE_LIMIT));

    }

    public static Geometry bufferProjected(Geometry geom,
                                           double meters, BufferParameters bufferParameters) {
        if (geom.isEmpty()) {
            return geom;
        }
        try {
            return bufferProjected(getLocalCRS(geom), geom, meters, bufferParameters);
        } catch (FactoryException e) {
            log.error("Failed to buffer geometry", e);
            return geom;
        }
    }

    /**
     * Builds a correct buffer around given geometry with given width in meters.
     *
     * You can not simply make a buffer around geometry which is in WGS84 coordinates, as 1 degree of lat and lon have different lengths in meters
     * depending on point on Earth. Such buffer will look deformed (iths width and height will not be equal).
     * So geometry needs to be projected to some plain 2d CRS first, then buffered, then projected back to WGS84
     */
    public static Geometry bufferProjected(
            CoordinateReferenceSystem localCrs,
            Geometry geom,
            double meters,
            BufferParameters bufferParameters) {
        try {
            if (geom.isEmpty()) {
                return geom;
            }

            geom = (Geometry) geom.clone();

            val globalToLocal = CRS.findMathTransform(DefaultGeographicCRS.WGS84, localCrs);
            val localToGlobal = CRS.findMathTransform(localCrs, DefaultGeographicCRS.WGS84);

            Geometry projectedGeom = JTS.transform(geom, globalToLocal);
            // buffer
            Geometry projectedBufferedGeom =  BufferOp.bufferOp(projectedGeom, meters, bufferParameters);
            // reproject the geometry to the original projection

            return JTS.transform(projectedBufferedGeom, localToGlobal);
        } catch (Exception ex) {
            log.error("Failed to buffer geometry", ex);
            return geom;
        }
    }

    /**
     * Simplifies geometry, collapsing points that are less than meters from each other
     * Projects to Mercator, simplifies, projects back
     * @param geom Geom in WGS84
     * @param meters Minimal in meters between points
     * @return Simplified geom in WGS84
     */
    public static Geometry simplifyProjected(Geometry geom, double meters) {
        if (geom == null || geom.isEmpty()) {
            return geom;
        }
        Geometry projected = transformToMercator(geom);
        TopologyPreservingSimplifier simplifier = new TopologyPreservingSimplifier(projected);
        simplifier.setDistanceTolerance(meters);
        Geometry projectedSimplified = simplifier.getResultGeometry();
        return transformFromMercator(projectedSimplified);
    }

    /**
     * Projects given polygon to 2d plane and calculates its area in square meters
     */
    public static double calcArea(Geometry geometry) {
        try {
            if (geometry.isEmpty()){
                return geometry.getArea();
            } else {
                val projed = JTS.transform(geometry, getLocalCRSTransform(geometry));
                return projed.getArea();
            }
        } catch (Exception ex) {
            log.error("Failed to calc area", ex);
            return geometry.getArea();
        }
    }

    /**
     * Projects given line to 2d plane and calculates its length in meters
     */
    public static double calcLength(LineString line) {
        try {
            if (line.isEmpty()){
                return line.getLength();
            } else {
                val projed = JTS.transform(line, getLocalCRSTransform(line));
                return projed.getLength();
            }
        } catch (Exception ex) {
            log.error("Failed to calc area", ex);
            return line.getLength();
        }
    }


    /**
     * Builds a correct round buffer around given point. Suitable for making circular areas on the map look really circular
     * (as a circle in WGS84 coordinates will not be a circle on the map but an ellipse)
     */
    public static Geometry makePointBuffer(Coordinate coord, double meters) {
        Point point = makePoint(coord);
        return bufferProjected(point, meters);
    }

    /**
     * Converts given envelope coordinates from WGS84 to Mercator
     */
    public static Envelope transformToMercator(Envelope latLonEnvelope) {

        Point bottomLeft = makePoint(new Coordinate(latLonEnvelope.getMinX(), latLonEnvelope.getMinY()));
        Point topRight = makePoint(new Coordinate(latLonEnvelope.getMaxX(), latLonEnvelope.getMaxY()));

        try {
            return new Envelope(
                    JTS.transform(bottomLeft, latLonToXY).getCoordinate(),
                    JTS.transform(topRight, latLonToXY).getCoordinate()
            );
        } catch (TransformException e) {
            log.error("Failed to transform", e);
            return latLonEnvelope;
        }
    }

    /**
     * Transforms given geometry from  WGS84 to Mercator
     * Remember that Mercator deforms distances greatly! Do not use resulting geometry to calculate any geometry properties like
     * area or length! Use other methods from this class, or convert geometry to most suitable local 2D CRS first using CRSUtils.getLocalCRS()
     */
    public static Geometry transformToMercator(Geometry geometry) {
        try {
            return JTS.transform(geometry, latLonToXY);
        } catch (TransformException e) {
            log.error("Failed to transsform", e);
            return geometry;
        }
    }

    public static Geometry transformToLocalCRS(Geometry geometry) throws FactoryException, TransformException {
        if (geometry.isEmpty()){
            return geometry;
        }
        return JTS.transform(geometry, getLocalCRSTransform(geometry));
    }

    public static Geometry transformToLocalCRS(CoordinateReferenceSystem crs, Geometry geometry) throws FactoryException, TransformException {
        if (geometry.isEmpty()){
            return geometry;
        }
        return JTS.transform(geometry, CRS.findMathTransform(DefaultGeographicCRS.WGS84, crs));
    }

    public static Coordinate transformToLocalCRS(Coordinate coordinate) {
        try {
            return transformToLocalCRS(getLocalCRS(makePoint(coordinate)), coordinate);
        } catch (FactoryException e) {
            log.error("Failed to transform", e);
            return coordinate;
        }
    }

    public static Coordinate transformToLocalCRS(CoordinateReferenceSystem crs, Coordinate coordinate) {
        try {
            Coordinate dest = new Coordinate();
            return JTS.transform(coordinate, dest, CRS.findMathTransform( DefaultGeographicCRS.WGS84, crs));
        } catch (TransformException | FactoryException e) {
            log.error("Failed to transform", e);
            return coordinate;
        }
    }

    public static Coordinate transformFromLocalCRS(CoordinateReferenceSystem crs, Coordinate coordinate) {
        try {
            Coordinate dest = new Coordinate();
            return JTS.transform(coordinate,
                    dest,
                    CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84));
        } catch (Exception e) {
            log.error("Failed to transform", e);
            return coordinate;
        }
    }

    public static Coordinate transformFromLocalCRS(Coordinate coordinate) {
        try {
            Coordinate dest = new Coordinate();
            return JTS.transform(coordinate,
                    dest,
                    CRS.findMathTransform(getLocalCRS(makePoint(coordinate)), DefaultGeographicCRS.WGS84));
        } catch (Exception e) {
            log.error("Failed to transform", e);
            return coordinate;
        }
    }

    public static Geometry transformFromLocalCRS(CoordinateReferenceSystem crs, Geometry geometry) {
        if (geometry.isEmpty()) {
            return geometry;
        }
        try {
            return JTS.transform(geometry, CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84));
        } catch (Exception e) {
            log.error("Failed to transform", e);
            return geometry;
        }
    }

    public static Geometry transformFromLocalCRS(Geometry geometry) {
        if (geometry.isEmpty()) {
            return geometry;
        }
        try {
            return JTS.transform(geometry, CRS.findMathTransform(getLocalCRS(geometry), DefaultGeographicCRS.WGS84));
        } catch (Exception e) {
            log.error("Failed to transform", e);
            return geometry;
        }
    }

    /**
     * Transforms given geometry from Mercator to WGS84
     */
    public static Geometry transformFromMercator(Geometry geometry) {
        try {
            return JTS.transform(geometry, xyToLatLon);
        } catch (TransformException e) {
            log.error("Failed to transsform", e);
            return geometry;
        }
    }

    public static MinimumDiameter getProjectedDiameter(Geometry g) {
        Geometry projected = null;
        try {
            projected = transformToLocalCRS(g);
        } catch (Exception e) {
            log.error("Failed to transform ", e);
        }
        return new MinimumDiameter(projected);
    }

    public static double angleBetweenProjected(Coordinate firstEnd, Coordinate middle, Coordinate secondEnd) {
        return Angle.angleBetween(
                transformToLocalCRS(firstEnd),
                transformToLocalCRS(middle),
                transformToLocalCRS(secondEnd)
        );

    }

    public static double getMinWidthMeters(Geometry g) {
        MinimumDiameter minimumDiameter = getProjectedDiameter(g);
        return minimumDiameter.getLength();
    }

    /**
     * Gets a point on Earth, which is located at a given distance and heading from given point
     */
    public static Coordinate getCoordinate(Coordinate start, double distance, double angle) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        geodeticCalculator.setStartingGeographicPoint(start.x, start.y);
        geodeticCalculator.setDirection(angle, distance);
        return new Coordinate(geodeticCalculator.getDestinationGeographicPoint().getX(), geodeticCalculator.getDestinationGeographicPoint().getY());
    }


    /**
     * Makes a line (in WGS84) using a starting point, heading and length.
     * 0 degrees is pointed North, clockwise, angle in degrees
     */
    public static LineString makeLine(Coordinate start, double angle, double length) {
        Coordinate end = getCoordinate(start, length, angle);
        return GeometryUtils.makeLine(start, end);
    }

    public static Geometry makeCircle(Coordinate coordinate, double radius) {
        BufferParameters bufferParameters = new BufferParameters(4, BufferParameters.CAP_ROUND, BufferParameters.JOIN_ROUND, BufferParameters.DEFAULT_MITRE_LIMIT);
        return makeCircle(coordinate, radius, bufferParameters);
    }

    public static Geometry makeCircle(Coordinate coordinate, double radius, BufferParameters bufferParameters) {
        return bufferProjected(GeometryUtils.makePoint(coordinate), radius, bufferParameters);
    }

    public static double calcAzimuth(Coordinate c1, Coordinate c2) {
        GeodeticCalculator gc = new GeodeticCalculator();
        gc.setStartingGeographicPoint(c1.x, c1.y);
        gc.setDestinationGeographicPoint(c2.x, c2.y);

        return gc.getAzimuth();
    }

    public static double calcAzimuth(LineString ls) {
        Coordinate[] coordinates = ls.getCoordinates();
        if(coordinates.length == 0 || ls.isEmpty()) {
            return 0d;  //probably better throw exception
        }

        if(ls.getCoordinates().length != 2) {
            throw new IllegalArgumentException(
                    String.format("LineString with 2 coordinates expected; %d coordinates provided", coordinates.length)
            );
        }

        return calcAzimuth(ls.getCoordinateN(0), ls.getCoordinateN(1));
    }

    public static LineString increaseLineLength(LineString ls, double fraction) {
        try {
            val localCrs = CRSUtils.getLocalCRS(ls);
            return increaseLineLength(localCrs, ls, fraction);
        } catch (Exception ex) {
            log.error("Failed to increase line length", ex);
            return ls;
        }
    }

    /**
     * Accepts a LineString (in WGS84) consisting of two coordinates and increases its length by a fraction of its length.
     * E.g. when a 0.5 fraction is passed, the line will be prolonged by 25% of its length from each end.
     * */
    public static LineString increaseLineLength(CoordinateReferenceSystem localCrs, LineString ls, double fraction) {
        try {
            val globalToLocal = CRS.findMathTransform(DefaultGeographicCRS.WGS84, localCrs);
            val localToGlobal = CRS.findMathTransform(localCrs, DefaultGeographicCRS.WGS84);

            LineString lsLocal = (LineString) JTS.transform(ls, globalToLocal);
            LineString increased = GeometryUtils.increaseLineLength(lsLocal, fraction);
            return (LineString) JTS.transform(increased, localToGlobal);
        } catch (Exception ex) {
            log.error("Failed to increase line length", ex);
            return ls;
        }
    }

    /**
     * Finds nearest points in 2 geometries, projecting them to local CRS first
     */
    public static Coordinate[] nearestPoints(CoordinateReferenceSystem localCrs,
                                                 Geometry wgsGeometry1,
                                                 Geometry wgsGeometry2) {
        try {
            Geometry projected1 = transformToLocalCRS(localCrs, wgsGeometry1);
            Geometry projected2 = transformToLocalCRS(localCrs, wgsGeometry2);
            Coordinate[] nearestPointsLocal = DistanceOp.nearestPoints(projected1, projected2);
            return new Coordinate[]{
                    transformFromLocalCRS(localCrs, nearestPointsLocal[0]),
                    transformFromLocalCRS(localCrs, nearestPointsLocal[1])
            };
        } catch (Exception ex) {
            log.error("Failed to find nearest points", ex);
            return new Coordinate[] {new Coordinate(), new Coordinate()};
        }
    }

    public static Coordinate[] nearestPoints(Geometry wgsGeometry1, Geometry wgsGeometry2) {
        if (wgsGeometry1.isEmpty() || wgsGeometry2.isEmpty()) {
            log.error("nearestPoints called with empty geometry");
            return new Coordinate[] {new Coordinate(), new Coordinate()};
        }
        try {
            return nearestPoints(getLocalCRS(wgsGeometry1), wgsGeometry1, wgsGeometry2);
        } catch (Exception ex) {
            log.error("Failed to find nearest points", ex);
            return new Coordinate[] {new Coordinate(), new Coordinate()};
        }
    }

    public static Coordinate nearestPoint(Coordinate wgsCoordinate, Geometry wgsGeometry) {
        return nearestPoints(
                GeometryUtils.makePoint(wgsCoordinate),
                wgsGeometry
        )[1];
    }

    public static Coordinate nearestPoint(CoordinateReferenceSystem localCrs,
                                          Coordinate wgsCoordinate,
                                          Geometry wgsGeometry) {
        return nearestPoints(
                localCrs,
                GeometryUtils.makePoint(wgsCoordinate),
                wgsGeometry
        )[1];
    }

    public static double getDistance(Geometry wgsGeometry1, Geometry wgsGeometry2) {
        if (wgsGeometry1.isEmpty() || wgsGeometry2.isEmpty()) {
            return 0.0;
        }
        Coordinate[] nearestPoints = nearestPoints(wgsGeometry1, wgsGeometry2);
        return getDistance(nearestPoints[0], nearestPoints[1]);
    }

    public static double getDistance(CoordinateReferenceSystem localCrs,
                                     Geometry wgsGeometry1,
                                     Geometry wgsGeometry2) {
        if (wgsGeometry1.isEmpty() || wgsGeometry2.isEmpty()) {
            return 0.0;
        }
        Coordinate[] nearestPoints = nearestPoints(localCrs, wgsGeometry1, wgsGeometry2);
        return getDistance(nearestPoints[0], nearestPoints[1]);
    }

    public static double getDistance(Coordinate first, Coordinate second) {
        return getDistance(first.y, first.x, second.y, second.x);
    }

    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        GeodeticCalculator gc = new GeodeticCalculator();
        gc.setStartingGeographicPoint(lon1, lat1);
        gc.setDestinationGeographicPoint(lon2, lat2);
        return gc.getOrthodromicDistance();
    }
}
