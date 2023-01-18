package ru.itmo.idu.geometry;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

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
}