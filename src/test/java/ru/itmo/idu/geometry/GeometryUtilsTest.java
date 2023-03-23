package ru.itmo.idu.geometry;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeometryUtilsTest {

    @Test
    public void testAngleToAzimuth() {
        double azimuth = -2;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        assertEquals(358, azimuth, 0.001);

        azimuth = -362;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        assertEquals(358, azimuth, 0.001);

        azimuth = 0;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        assertEquals(0, azimuth, 0.001);

        azimuth = 360;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        assertEquals(0, azimuth, 0.001);

        azimuth = 2;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        assertEquals(2, azimuth, 0.001);

        azimuth = 362;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        assertEquals(2, azimuth, 0.001);
    }

    @Test
    void increaseLineLength() {
        Coordinate c1 = new Coordinate(0,0);
        Coordinate c2 = new Coordinate(0, 10);

        LineString ls = GeometryUtils.makeLine(c1, c2);
        LineString result = GeometryUtils.increaseLineLength(ls, 0.5);

        Coordinate[] newCoords = result.getCoordinates();
        assertEquals(0.0, newCoords[0].x, 0.001);
        assertEquals(-2.5, newCoords[0].y, 0.001);
        assertEquals(0.0, newCoords[1].x, 0.001);
        assertEquals(12.5, newCoords[1].y, 0.001);

        ls = GeometryUtils.geometryFactory.createLineString();
        result = GeometryUtils.increaseLineLength(ls, 0.5);
        assertTrue(result.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> {
            Coordinate c3 = new Coordinate(5, 10);
            LineString ls1 = GeometryUtils.geometryFactory.createLineString(new Coordinate[]{c1, c2, c3});
            GeometryUtils.increaseLineLength(ls1, 0.5);
        });
    }

    @Test
    public void testMakePolygonFromCoordinates() {
        List<Coordinate> list = new ArrayList<>();
        list.add(new Coordinate(30.28687717, 59.72104526));
        list.add(new Coordinate(30.28917225, 59.72028718));
        list.add(new Coordinate(30.28788596, 59.71929357));
        Geometry geomUnited = GeometryUtils.makePolygonFromCoordinates(list, 2.0);
        assertFalse(geomUnited.isEmpty());
    }

    @Test
    void testMakePoint() {
        Point p1 = GeometryUtils.makePoint(10, 20);
        Point p2 = GeometryUtils.makePoint(new Coordinate(10, 20));

        assertEquals(p1, p2);
        assertEquals(10.0, p1.getX());
        assertEquals(20.0, p2.getY());
    }

    @Test
    public void testMakeLineWorksWith3DLines() {
        LineString ls = GeometryUtils.makeLine(new Coordinate(1.0, 1.0, 100.0), new Coordinate(1.0, 1.0, 200.0));
        assertEquals(100.0, ls.getCoordinates()[0].z);

        ls = GeometryUtils.makeLine(new double[][]{{1.0, 1.0, 100.0}, {1.0, 1.0, 200.0}});
        assertEquals(200.0, ls.getCoordinates()[1].z);
    }

    @Test
    public void testFlattenGeometry() {
        List<Geometry> rz = GeometryUtils.flattenGeometry(null);
        assertTrue(rz.isEmpty());

        rz = GeometryUtils.flattenGeometry(GeometryUtils.geometryFactory.createEmpty(2));
        assertTrue(rz.isEmpty());

        rz = GeometryUtils.flattenGeometry(GeometryUtils.makePoint(10, 10));
        assertEquals(1, rz.size());

        rz = GeometryUtils.flattenGeometry(GeometryUtils.geometryFactory.createGeometryCollection(new Geometry[]{
                GeometryUtils.makePoint(1, 1),
                GeometryUtils.geometryFactory.createGeometryCollection(new Geometry[]{
                        GeometryUtils.makePoint(3, 3),
                        GeometryUtils.makePoint(5, 5),
                })
        }));

        assertEquals(3, rz.size());
        assertEquals(9, rz.stream().mapToDouble(it -> it.getCoordinate().x).sum(), 0.001);
    }

    @Test
    public void testConvertGeometryCollections() {
        Geometry gc = GeometryUtils.geometryFactory.createGeometryCollection(new Geometry[]{
            GeometryUtils.makeLine(new Coordinate(1, 1), new Coordinate(2, 2)),
            GeometryUtils.makeLine(new Coordinate(1, 1), new Coordinate(2, 2)),
        });

        Geometry rz = GeometryUtils.tryConvertGCToCorrectSubclass(gc);
        assertEquals(MultiLineString.class, rz.getClass());
        assertEquals(2, rz.getNumGeometries());

        gc = GeometryUtils.geometryFactory.createGeometryCollection(new Geometry[]{
                GeometryUtils.makePoint(new Coordinate(1, 1)),
                GeometryUtils.makePoint(new Coordinate(1, 1)).buffer(1),
        });
        rz = GeometryUtils.tryConvertGCToCorrectSubclass(gc);
        assertEquals(rz, gc);
    }
}