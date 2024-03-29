package ru.itmo.idu.geometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.valid.IsValidOp;

import static ru.itmo.idu.geometry.GeometryUtils.geometryFactory;

public class SafeOperations {

    private static Geometry fixGeometry(Geometry geom) {
        if (geom == null) {
            return geometryFactory.createEmpty(2);
        }
        if (geom.isEmpty()) {
            return geom;
        }
        if (geom.getClass() == GeometryCollection.class) {
            // many geometry operations do not work properly on geometry collection arguments, but work on multipolygons
            // do not use instanceof since MultiLineString is also a GeometryCollection, but buffering will turn it into empty polygon
            geom = GeometryUtils.tryConvertGCToCorrectSubclass(geom);

            if (geom.getClass() == GeometryCollection.class) {
                // failed to covert, GC contains features of different types
                // this can still provide invalid result if GC contains both polygons and lines - lines will be wiped out
                geom = geom.buffer(0.0);
            }
        }
        IsValidOp validOp = new IsValidOp(geom);
        if (validOp.isValid()) {
            return geom;
        }
        Geometry rz = GeometryUtils.fixGeometry(geom, validOp.getValidationError());
        if (rz == null) {
            return geom;
        }
        return rz;
    }

    public static Boolean safeIntersects(PreparedGeometry geom1, Geometry geom2, double bufferMeters){
        Geometry geom2Fixed = fixGeometry(geom2);
        try {
            return geom1.intersects(geom2Fixed);
        } catch (TopologyException tpe) {
            try {
                return ProjectionUtils.bufferProjected(geom1.getGeometry(), bufferMeters).intersects(geom2Fixed);
            } catch (TopologyException tpe2) {
                return false;
            }
        }
    }

    public static Boolean safeIntersects(PreparedGeometry geom1, Geometry geom2){
        return safeIntersects(geom1, geom2, 1.0);
    }

    public static Boolean safeIntersects(Geometry geom1, Geometry geom2, double bufferMeters) {
        Geometry geom1Fixed = fixGeometry(geom1);
        PreparedGeometryFactory factory = new PreparedGeometryFactory();
        PreparedGeometry geom1Prepared = factory.create(geom1Fixed);
        return safeIntersects(geom1Prepared, geom2, bufferMeters);
    }

    public static Boolean safeIntersects(Geometry geom1, Geometry geom2){
        return safeIntersects(geom1, geom2, 1.0);
    }

    public static Geometry safeIntersection(PreparedGeometry geom1, Geometry geom2, double bufferMeters) {
        if (geom1.getGeometry().isEmpty() || geom2.isEmpty()) {
            return geometryFactory.createEmpty(2);
        }
        Geometry geom2Fixed = fixGeometry(geom2);
        try {
            return geom1.getGeometry().intersection(geom2Fixed);
        } catch (TopologyException tpe) {
            try {
                return ProjectionUtils.bufferProjected(geom1.getGeometry(), bufferMeters).intersection(geom2Fixed);
            } catch (TopologyException tpe2) {
                return geometryFactory.createEmpty(2);
            }
        }
    }

    public static Geometry safeIntersection(PreparedGeometry geom1, Geometry geom2) {
        return safeIntersection(geom1, geom2, 1.0);
    }

    public static Geometry safeIntersection(Geometry geom1, Geometry geom2, double bufferMeters) {
        if (geom1.isEmpty() || geom2.isEmpty()) {
            return geometryFactory.createEmpty(2);
        }
        Geometry geom1Fixed = fixGeometry(geom1);
        PreparedGeometryFactory factory = new PreparedGeometryFactory();
        PreparedGeometry geom1Prepared = factory.create(geom1Fixed);
        return safeIntersection(geom1Prepared, geom2, bufferMeters);
    }

    public static Geometry safeIntersection(Geometry geom1, Geometry geom2) {
        return safeIntersection(geom1, geom2, 1.0);
    }

    public static Geometry safeDifference(PreparedGeometry geom1, Geometry geom2, double bufferMeters) {
        Geometry geom2Fixed = fixGeometry(geom2);
        final boolean isGeometryCollection = geom1.getGeometry().getNumGeometries() > 1 || geom2.getNumGeometries() > 1;
        try {
            if (isGeometryCollection) {
                return GeometryUtils.geometryCollectionDifference(geom1.getGeometry(), geom2);
            } else {
                return geom1.getGeometry().difference(geom2Fixed);
            }
        } catch (TopologyException tpe) {
            try {

                final Geometry slightlyBufferedFirst = ProjectionUtils.bufferProjected(geom1.getGeometry(), bufferMeters);
                if (isGeometryCollection) {
                    return slightlyBufferedFirst.difference(geom2Fixed);
                } else {
                    return slightlyBufferedFirst.difference(geom2Fixed);
                }
            } catch (TopologyException tpe2) {
                return geometryFactory.createEmpty(2);
            }
        }
    }

    public static Geometry safeDifference(PreparedGeometry geom1, Geometry geom2) {
        return safeDifference(geom1, geom2, 1.0);
    }

    public static Geometry safeDifference(Geometry geom1, Geometry geom2, double bufferMeters) {
        Geometry geom1Fixed = fixGeometry(geom1);
        PreparedGeometryFactory factory = new PreparedGeometryFactory();
        PreparedGeometry geom1Prepared = factory.create(geom1Fixed);
        return safeDifference(geom1Prepared, geom2, bufferMeters);
    }

    public static Geometry safeDifference(Geometry geom1, Geometry geom2) {
        return safeDifference(geom1, geom2, 1.0);
    }

    public static Geometry safeUnion(PreparedGeometry geom1, Geometry geom2, double bufferMeters) {
        Geometry geom2Fixed = fixGeometry(geom2);
        try {
            return geom1.getGeometry().union(geom2Fixed);
        } catch (TopologyException tpe) {
            try {
                return ProjectionUtils.bufferProjected(geom1.getGeometry(), bufferMeters).union(geom2Fixed);
            } catch (TopologyException tpe2) {
                return geometryFactory.createEmpty(2);
            }
        }
    }

    public static Geometry safeUnion(PreparedGeometry geom1, Geometry geom2) {
        return safeUnion(geom1, geom2, 1.0);
    }

    public static Geometry safeUnion(Geometry geom1, Geometry geom2, double bufferMeters) {
        Geometry geom1Fixed = fixGeometry(geom1);
        PreparedGeometryFactory factory = new PreparedGeometryFactory();
        PreparedGeometry geom1Prepared = factory.create(geom1Fixed);
        return safeUnion(geom1Prepared, geom2, bufferMeters);
    }

    public static Geometry safeUnion(Geometry geom1, Geometry geom2) {
        return safeUnion(geom1, geom2, 1.0);
    }

    public static Boolean safeCovers(PreparedGeometry geom1, Geometry geom2, double bufferMeters){
        Geometry geom2Fixed = fixGeometry(geom2);
        try {
            return geom1.covers(geom2Fixed);
        } catch (TopologyException tpe) {
            try {
                return ProjectionUtils.bufferProjected(geom1.getGeometry(), bufferMeters).covers(geom2Fixed);
            } catch (TopologyException tpe2) {
                return false;
            }
        }
    }

    public static Boolean safeCovers(PreparedGeometry geom1, Geometry geom2) {
        return safeCovers(geom1, geom2, 1.0);
    }

    public static Boolean safeCovers(Geometry geom1, Geometry geom2, double bufferMeters){
        Geometry geom1Fixed = fixGeometry(geom1);
        PreparedGeometryFactory factory = new PreparedGeometryFactory();
        PreparedGeometry geom1Prepared = factory.create(geom1Fixed);
        return safeCovers(geom1Prepared, geom2, bufferMeters);
    }

    public static Boolean safeCovers(Geometry geom1, Geometry geom2) {
        return safeCovers(geom1, geom2, 1.0);
    }

    public static Boolean safeContains(PreparedGeometry geom1, Geometry geom2, double bufferMeters){
        Geometry geom2Fixed = fixGeometry(geom2);
        try {
            return geom1.contains(geom2Fixed);
        } catch (TopologyException tpe) {
            try {
                return ProjectionUtils.bufferProjected(geom1.getGeometry(), bufferMeters).contains(geom2Fixed);
            } catch (TopologyException tpe2) {
                return false;
            }
        }
    }

    public static Boolean safeContains(PreparedGeometry geom1, Geometry geom2) {
        return safeContains(geom1, geom2, 1.0);
    }

    public static Boolean safeContains(Geometry geom1, Geometry geom2, double bufferMeters){
        Geometry geom1Fixed = fixGeometry(geom1);
        PreparedGeometryFactory factory = new PreparedGeometryFactory();
        PreparedGeometry geom1Prepared = factory.create(geom1Fixed);
        return safeContains(geom1Prepared, geom2, bufferMeters);
    }

    public static Boolean safeContains(Geometry geom1, Geometry geom2) {
        return safeContains(geom1, geom2, 1.0);
    }

}
