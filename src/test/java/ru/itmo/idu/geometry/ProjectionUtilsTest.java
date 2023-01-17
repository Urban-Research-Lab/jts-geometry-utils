package ru.itmo.idu.geometry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionUtilsTest {

    @Test
    void increaseLineLength() throws FactoryException, TransformException {
        Coordinate c1 = new Coordinate(30.474017775990433, 59.88608461148712);
        Coordinate c2 = new Coordinate(30.491236123144063, 59.88858199271934);

        LineString ls = GeometryUtils.makeLine(c1, c2);
        double length = ProjectionUtils.calcLength(ls);

        LineString lsIncreased = ProjectionUtils.increaseLineLength(ls, 0.5);
        double lengthIncreased = ProjectionUtils.calcLength(lsIncreased);

        Assertions.assertEquals(1.5, lengthIncreased / length, 0.01);
    }

}