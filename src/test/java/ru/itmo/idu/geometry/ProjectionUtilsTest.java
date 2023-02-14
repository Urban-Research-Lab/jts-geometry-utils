package ru.itmo.idu.geometry;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.opengis.feature.Feature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.itmo.idu.geometry.GeometryUtils.geometryFactory;

public class ProjectionUtilsTest {

    @Test
    public void transformToLocalCRSTest() throws FactoryException, TransformException, IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        assertTrue(ProjectionUtils.transformToLocalCRS(geom1).isEmpty());
        CoordinateReferenceSystem localCrs = CRSUtils.getLocalCRS(readGeometryFromGeoJSON("polygonIntersects1.json"));
        assertTrue(ProjectionUtils.transformToLocalCRS(localCrs, geom1).isEmpty());
        assertTrue(ProjectionUtils.transformFromLocalCRS(localCrs, geom1).isEmpty());
        assertTrue(ProjectionUtils.transformFromLocalCRS(geom1).isEmpty());
    }

    @Test
    public void calcAreaTest() {
        Geometry geom1 = geometryFactory.createEmpty(2);
        assertEquals(0.0, ProjectionUtils.calcArea(geom1), 0.01);
    }

    @Test
    public void bufferProjectedTest() throws IOException, FactoryException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        assertTrue(ProjectionUtils.bufferProjected(geom1, 2.0).isEmpty());
        CoordinateReferenceSystem localCrs = CRSUtils.getLocalCRS(readGeometryFromGeoJSON("polygonIntersects1.json"));
        assertTrue(ProjectionUtils.bufferProjected(localCrs, geom1, 2.0).isEmpty());
        assertTrue(ProjectionUtils.simplifyProjected(geom1, 2.0).isEmpty());
    }

    protected Geometry readGeometryFromGeoJSON(String resourceName) throws IOException {
        FeatureCollection fc = new FeatureJSON().readFeatureCollection(getClass().getClassLoader().getResourceAsStream(resourceName));

        Feature next = fc.features().next();
        return (Geometry) next.getDefaultGeometryProperty().getValue();
    }

    @Test
    public void testMakeCircle() {
        Coordinate coordinate = new Coordinate(30.5413682568238, 59.88265603527486);
        Geometry circle = ProjectionUtils.makeCircle(coordinate, 10d);

        double area = ProjectionUtils.calcArea(circle);
        assertEquals(306d, area, 1.0);

        BufferParameters bufferParameters = new BufferParameters(8, BufferParameters.CAP_ROUND, BufferParameters.JOIN_ROUND, BufferParameters.DEFAULT_MITRE_LIMIT);
        circle = ProjectionUtils.makeCircle(coordinate, 10d, bufferParameters);
        area = ProjectionUtils.calcArea(circle);
        assertEquals(312d, area, 1.0);
    }

    @Test
    public void testCalcAzimuth() {
        Coordinate c1 = new Coordinate(30.5413682568238, 59.88265603527486);
        Coordinate c2 = new Coordinate(30.54140787107076, 59.87633443882797);

        double azimuth = ProjectionUtils.calcAzimuth(c1, c2);
        assertEquals(180d, azimuth, 0.5);
    }

    @Test
    void increaseLineLength() throws FactoryException, TransformException {
        Coordinate c1 = new Coordinate(30.474017775990433, 59.88608461148712);
        Coordinate c2 = new Coordinate(30.491236123144063, 59.88858199271934);

        LineString ls = GeometryUtils.makeLine(c1, c2);
        double length = ProjectionUtils.calcLength(ls);

        LineString lsIncreased = ProjectionUtils.increaseLineLength(ls, 0.5);
        double lengthIncreased = ProjectionUtils.calcLength(lsIncreased);

        assertEquals(1.5, lengthIncreased / length, 0.01);
    }

    @Test
    void testNearestPoint() {
        Geometry empty = geometryFactory.createEmpty(2);
        Geometry firstPoint = GeometryUtils.makePoint(30.474017775990433, 59.88608461148712);
        Geometry secondPoint = GeometryUtils.makePoint(30.491236123144063, 59.88858199271934);

        Coordinate[] rz1 = ProjectionUtils.nearestPoints(empty, firstPoint);
        assertEquals(0.0, rz1[0].x, 0.001);
        assertEquals(0.0, rz1[0].y, 0.001);

        Coordinate[] rz2 = ProjectionUtils.nearestPoints(firstPoint, secondPoint);

        assertTrue(firstPoint.getCoordinate().equals2D(rz2[0], 0.000001));
        assertTrue(secondPoint.getCoordinate().equals2D(rz2[1], 0.000001));

        Coordinate rz3 = ProjectionUtils.nearestPoint(secondPoint.getCoordinate(), firstPoint);
        assertTrue(rz3.equals2D(firstPoint.getCoordinate(), 0.000001));
    }

    @Test
    void testDistance() {
        GeodeticCalculator gc = new GeodeticCalculator();
        gc.setStartingGeographicPoint(20.10, -10.15);
        gc.setDirection(34, 25.5);

        Coordinate start = new Coordinate(gc.getStartingGeographicPoint().getX(), gc.getStartingGeographicPoint().getY());
        Coordinate end = new Coordinate(gc.getDestinationGeographicPoint().getX(), gc.getDestinationGeographicPoint().getY());

        assertEquals(25.5, ProjectionUtils.getDistance(start, end), 0.001);
        assertEquals(25.5, ProjectionUtils.getDistance(start.y, start.x, end.y, end.x), 0.001);
        assertEquals(25.5, ProjectionUtils.getDistance(end, start), 0.001);
        assertEquals(0.0, ProjectionUtils.getDistance(start, start), 0.001);
    }

    @Test
    void testDistanceGeometries() {
        GeodeticCalculator gc = new GeodeticCalculator();
        gc.setStartingGeographicPoint(20.10, -10.15);
        gc.setDirection(90, 10.0);

        Coordinate start = new Coordinate(gc.getStartingGeographicPoint().getX(), gc.getStartingGeographicPoint().getY());
        Coordinate end = new Coordinate(gc.getDestinationGeographicPoint().getX(), gc.getDestinationGeographicPoint().getY());

        final Geometry startCircle = ProjectionUtils.makeCircle(start, 2.0);
        final Geometry endCircle = ProjectionUtils.makeCircle(end, 2.0);
        assertEquals(6.0, ProjectionUtils.getDistance(startCircle, endCircle), 0.1);
        assertEquals(0.0, ProjectionUtils.getDistance(startCircle, ProjectionUtils.bufferProjected(startCircle, 1.0)), 0.1);

        final Geometry empty = geometryFactory.createEmpty(2);
        assertEquals(0.0, ProjectionUtils.getDistance(empty, endCircle), 0.1);


    }
}
