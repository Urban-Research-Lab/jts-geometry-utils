package ru.itmo.idu.geometry;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PointsGridGeneratorTest {

    @Test
    void generatePoints() throws FactoryException, TransformException {
        Geometry polygon = GeometryUtils.makePolygon(
                new Coordinate(30.529875152102818, 59.9017412528203),
                new Coordinate(30.529768819463612, 59.89774169966455),
                new Coordinate(30.537177463764948, 59.89766857856378),
                new Coordinate(30.537094135302652, 59.899803611085986),
                new Coordinate(30.533053495003486, 59.89982138683976),
                new Coordinate(30.533088939216583, 59.9016344637553),
                new Coordinate(30.529875152102818, 59.9017412528203)
        );

        CoordinateReferenceSystem localCRS = CRSUtils.getLocalCRS(polygon.getCentroid());
        List<Point> points1 = PointsGridGenerator.generatePoints(localCRS, polygon, 10.0, 0.0);
        List<Point> points2 = PointsGridGenerator.generatePoints(localCRS, polygon, 10.0, 20.0);

        assertTrue(points1.size() > 0);
        assertTrue(points2.size() > 0);
        assertNotEquals(points1.size(), points2.size());
    }

}