package ru.itmo.idu.geometry;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ru.itmo.idu.geometry.algorithms.LloydAlgorithm;

import java.util.Arrays;
import java.util.Comparator;

public class LloydAlgorithmTest {

    @Test
    public void testLloydPoints() throws FactoryException, TransformException {
        // make a 10x10 meters rect with 100 points at 1 meter distance
        Geometry area = ProjectionUtils.makeAABB(new Coordinate(30, 60), 10.0, 10.0);
        Coordinate[] lloydPoints = LloydAlgorithm.generateLloydPointsWGS84(CRSUtils.getLocalCRS(area), area, 1.0);
        Assertions.assertEquals(100, lloydPoints.length);

        double averageDist = 0.0;
        final int pointsCount = 10;

        for (int i = 0; i < pointsCount; ++i) {
            Coordinate randomPoint = lloydPoints[RandomUtils.nextInt(0, lloydPoints.length)];
            Coordinate nearest = Arrays.stream(lloydPoints)
                    .filter(c -> c != randomPoint)
                    .min(Comparator.comparingDouble(c -> ProjectionUtils.getDistance(c, randomPoint)))
                    .orElseThrow();

            averageDist += ProjectionUtils.getDistance(nearest, randomPoint);
        }
        Assertions.assertEquals(1.0, averageDist / pointsCount, 0.1);

    }
}
