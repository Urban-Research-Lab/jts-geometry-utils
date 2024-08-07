package ru.itmo.idu.geometry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.Collections;
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
    public void testMakeRectangle() {
        Polygon p = GeometryUtils.makeRectangle(10, 10, 30, 40);
        Envelope e = p.getEnvelopeInternal();
        assertEquals(10, e.getMinX());
        assertEquals(50, e.getMaxY());
        assertEquals(1200.0, p.getArea(), 0.1);
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
    void testMakePointWithDirection() {
        Coordinate c1 = new Coordinate(0.0, 0.0);

        Coordinate c2 = GeometryUtils.makeCoordinateFromDirection(c1, 1.0, Math.PI / 2);

        assertEquals(0.0, c2.x, 0.0001);
        assertEquals(1.0, c2.y, 0.0001);
    }

    @Test
    public void testMakeLineWorksWith3DLines() {
        LineString ls = GeometryUtils.makeLine(new Coordinate(1.0, 1.0, 100.0), new Coordinate(1.0, 1.0, 200.0));
        assertEquals(100.0, ls.getCoordinates()[0].z);

        ls = GeometryUtils.makeLine(new double[][]{{1.0, 1.0, 100.0}, {1.0, 1.0, 200.0}});
        assertEquals(200.0, ls.getCoordinates()[1].z);
    }

    @Test
    public void testMakeLineDirection() {
        LineString ls = GeometryUtils.makeLineFromDirection(0.0, 0.0, 1.0, Math.PI / 4.0);
        assertEquals(new Coordinate(0.0, 0.0), ls.getStartPoint().getCoordinate());
        final Coordinate expectedEnd = new Coordinate(1.0 / Math.sqrt(2), 1.0 / Math.sqrt(2));
        assertEquals(expectedEnd.x, ls.getEndPoint().getCoordinate().x, 0.0001);
        assertEquals(expectedEnd.y, ls.getEndPoint().getCoordinate().y, 0.0001);
        assertEquals(1.0, ls.getLength(), 0.001);
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

    @Test
    public void testCreateMultipoint() {
        var mp = GeometryUtils.makeMultiPoint(GeometryUtils.makePoint(0, 0), GeometryUtils.makePoint(1, 1));
        Assertions.assertEquals(2, mp.getNumGeometries());

        var mp2 = GeometryUtils.makeMultiPoint(new Coordinate(0, 0), new Coordinate(1, 1));
        Assertions.assertEquals(mp, mp2);
    }

    @Test
    public void testCreateLine() {
        final Coordinate start = new Coordinate(0, 1);
        final Coordinate end = new Coordinate(1, 2);
        var line = GeometryUtils.makeLine(start, end);
        assertEquals(2, line.getCoordinates().length);
        assertEquals(0.0, line.getCoordinates()[0].getX());
        assertEquals(2.0, line.getCoordinates()[1].getY());

        var line2 = GeometryUtils.makeLine(List.of(start, end));
        assertEquals(line, line2);

        var line3 = GeometryUtils.makeLine(start.x, start.y, end.x, end.y);
        assertEquals(line3, line2);

        var coords = new double[][] {
                new double[] {start.x, start.y},
                new double[] {end.x, end.y},
        };
        var line4 = GeometryUtils.makeLine(coords);
        assertEquals(line4, line);
    }

    @Test
    public void testCreateTrivialLine() {
        var emptyLine = GeometryUtils.makeLine(Collections.emptyList());
        assertTrue(emptyLine.isEmpty());

        final Coordinate point = new Coordinate(1, 2);
        var singlePointLine = GeometryUtils.makeLine(List.of(point));
        assertEquals(2, singlePointLine.getCoordinates().length);
        assertEquals(point, singlePointLine.getCoordinates()[0]);
        assertEquals(point, singlePointLine.getCoordinates()[1]);
    }

    @Test
    public void testCreateRing() {
        var ring = GeometryUtils.makeRing(new Coordinate(0, 0), new Coordinate(1, 1));
        assertEquals(3, ring.getCoordinates().length);
        assertEquals(0.0, ring.getCoordinates()[2].x, 0.1);
        assertEquals(1.0, ring.getCoordinates()[1].x, 0.1);
    }

    @Test
    public void testMakeEmpty() {
        var emptyPolygon = GeometryUtils.makeEmptyPolygon();
        Assertions.assertTrue(emptyPolygon.isEmpty());

        var emptyLine = GeometryUtils.makeEmptyLine();
        Assertions.assertTrue(emptyLine.isEmpty());

        var empty = GeometryUtils.makeEmpty();
        Assertions.assertTrue(empty.isEmpty());
        Assertions.assertTrue(empty.isValid());
    }

    @Test
    public void testGeometrySegmentList() {
        List<LineSegment> emptyList = GeometryUtils.geometrySegmentList(GeometryUtils.makeEmptyPolygon());
        Assertions.assertTrue(emptyList.isEmpty());

        List<LineSegment> pointList = GeometryUtils.geometrySegmentList(GeometryUtils.makePoint(0.0, 1.0));
        Assertions.assertTrue(pointList.isEmpty());

        List<LineSegment> simpleLineList = GeometryUtils.geometrySegmentList(GeometryUtils.makeLine(0.0, 0.0, 1.0, 1.0));
        Assertions.assertEquals(1, simpleLineList.size());
        Assertions.assertEquals(0.0, simpleLineList.get(0).p0.x, 0.001);
        Assertions.assertEquals(1.0, simpleLineList.get(0).p1.x, 0.001);

        List<LineSegment> multilineList = GeometryUtils.geometrySegmentList(
                GeometryUtils.makeGeometryCollection(
                        GeometryUtils.makeLine(0.0, 0.0, 1.0, 0.0),
                        GeometryUtils.makeLine(100.0, 100.0, 1000.0, 100.0)
                )
        );

        Assertions.assertEquals(2, multilineList.size());
        Assertions.assertEquals(1.0, multilineList.get(0).getLength());
        Assertions.assertEquals(900.0, multilineList.get(1).getLength());

        Polygon polygonWithHole = (Polygon) GeometryUtils.makeRectangle(0, 0, 10, 10)
                .difference(GeometryUtils.makeRectangle(3, 3, 3, 3));
        Assertions.assertEquals(1, polygonWithHole.getNumInteriorRing());

        List<LineSegment> polygonWithHoleList = GeometryUtils.geometrySegmentList(polygonWithHole);
        Assertions.assertEquals(8, polygonWithHoleList.size());
    }
}