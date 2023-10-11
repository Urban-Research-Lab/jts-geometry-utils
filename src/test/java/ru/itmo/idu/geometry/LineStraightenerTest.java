package ru.itmo.idu.geometry;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import ru.itmo.idu.geometry.algorithms.LineStraightener;

import java.util.List;

public class LineStraightenerTest {
    @Test
    public void testSimple() {
        var originalLine = GeometryUtils.makeLine(
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 0)
        );

        var boundary = originalLine.getEnvelope().buffer(10);

        LineString rz = LineStraightener.straightenLineString(
            originalLine,
            boundary,
            null
        );

        Assert.assertEquals(2, rz.getCoordinates().length);
        Assert.assertEquals(0, rz.getCoordinates()[0].x, 0.1);
        Assert.assertEquals(2, rz.getCoordinates()[1].x, 0.1);
    }

    @Test
    public void testSimpleNoBoundary() {
        var originalLine = GeometryUtils.makeLine(
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 0)
        );

        LineString rz = LineStraightener.straightenLineString(
                originalLine,
                null,
                null
        );

        Assert.assertEquals(2, rz.getCoordinates().length);
    }

    @Test
    public void test2PointsInLine() {
        var originalLine = GeometryUtils.makeLine(
                new Coordinate(0, 0),
                new Coordinate(1, 1)
        );

        var rz = LineStraightener.straightenLineString(
                originalLine,
                null,
                null
        );

        Assert.assertEquals(rz, originalLine);
    }

    @Test
    public void testKeepSpecialPoint() {
        var originalLine = GeometryUtils.makeLine(
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 0),
                new Coordinate(3, 1)
        );

        LineString rz = LineStraightener.straightenLineString(
                originalLine,
                null,
                List.of(new Coordinate(2, 0))
        );

        Assert.assertEquals(3, rz.getCoordinates().length);
        Assert.assertEquals(rz.getCoordinates()[0], originalLine.getCoordinates()[0]);
        Assert.assertEquals(rz.getCoordinates()[1], originalLine.getCoordinates()[2]);
        Assert.assertEquals(rz.getCoordinates()[2], originalLine.getCoordinates()[3]);
    }

    @Test
    public void testLastPointOutsideOfBorder() {
        var originalLine = GeometryUtils.makeLine(
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(2, 0),
                new Coordinate(3, 1)
        );

        LineString rz = LineStraightener.straightenLineString(
                originalLine,
                GeometryUtils.makePolygon(
                        new Coordinate(-1, -1),
                        new Coordinate(2.5, -1),
                        new Coordinate(2.5, 5),
                        new Coordinate(-1, 5)
                ),
                null
        );

        Assert.assertEquals(3, rz.getCoordinates().length);
        Assert.assertEquals(rz.getCoordinates()[0], originalLine.getCoordinates()[0]);
        Assert.assertEquals(rz.getCoordinates()[1], originalLine.getCoordinates()[2]);
        Assert.assertEquals(rz.getCoordinates()[2], originalLine.getCoordinates()[3]);
    }
}
