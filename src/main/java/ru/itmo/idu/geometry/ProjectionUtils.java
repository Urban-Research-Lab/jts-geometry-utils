package ru.itmo.idu.geometry;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
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
        return bufferProjected(geom, meters, new BufferParameters(16, BufferParameters.CAP_ROUND, BufferParameters.JOIN_ROUND, BufferParameters.DEFAULT_MITRE_LIMIT));

    }

    /**
     * Builds a correct buffer around given geometry with given width in meters.
     *
     * You can not simply make a buffer around geometry which is in WGS84 coordinates, as 1 degree of lat and lon have different lengths in meters
     * depending on point on Earth. Such buffer will look deformed (iths width and height will not be equal).
     * So geometry needs to be projected to some plain 2d CRS first, then buffered, then projected back to WGS84
     */
    public static Geometry bufferProjected(Geometry geom,
                                           double meters, BufferParameters bufferParameters) {
        try {
            geom = (Geometry) geom.clone();
            val invertCoordinateFilter = new InvertCoordinateFilter();
            geom.apply(invertCoordinateFilter);

            CoordinateReferenceSystem auto = getLocalCRS(geom);
            val globalToLocal = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
            val localToGlobal = CRS.findMathTransform(auto, DefaultGeographicCRS.WGS84);

            Geometry projectedGeom = JTS.transform(geom, globalToLocal);
            // buffer
            Geometry projectedBufferedGeom =  BufferOp.bufferOp(projectedGeom, meters, bufferParameters);
            // reproject the geometry to the original projection

            val rz = JTS.transform(projectedBufferedGeom, localToGlobal);
            rz.apply(invertCoordinateFilter);
            return rz;
        } catch (Exception ex) {
            log.error("Failed to buffer geometry", ex);
            return geom;
        }
    }



    /**
     * Projects given polygon to 2d plane and calculates its area in square meters
     */
    public static double calcArea(Geometry geometry) {
        try {
            val projed = JTS.transform(geometry, getLocalCRSTransform(geometry));
            return projed.getArea();
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
            val projed = JTS.transform(line, getLocalCRSTransform(line));
            return projed.getLength();
        } catch (Exception ex) {
            log.error("Failed to calc area", ex);
            return line.getLength();
        }
    }

    /**
     * Builds a correct round buffer around given point. Suitable for making circular areas on the map look really circular
     * (as a circle in WGS84 coordinates will not be a circle on the map but an ellipse)
     */
    public static Geometry makePointBuffer(Coordinate coord, int meters) {
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
        return JTS.transform(geometry, getLocalCRSTransform(geometry));
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


}
