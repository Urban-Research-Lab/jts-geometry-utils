package ru.itmo.idu.geometry;

import lombok.val;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Helper methods for converting one CRS into another
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CRSUtils {

    /**
     * Creates a transform, that can be used to project given geometry from WGS84 to local CRS with meters as X and Y
     */
    public static MathTransform getLocalCRSTransform(Geometry geometry) throws FactoryException {
        CoordinateReferenceSystem auto = getLocalCRS(geometry);
        return CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
    }

    /**
     * Finds a most suitable local CRS for given geometry, which can be used to convert lat-lon coords to meters
     * @param geometry Geometry with coordinates in WGS84
     * @return Most precise local CRS available for a location defined by geometry centroid point
     */

    public static CoordinateReferenceSystem getLocalCRS(Geometry geometry) throws FactoryException {
        Point centroid = geometry.getCentroid();
        String code = "AUTO:42001," + centroid.getX() + "," + centroid.getY();
        return CRS.decode(code);
    }
}
