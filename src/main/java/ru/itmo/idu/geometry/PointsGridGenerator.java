package ru.itmo.idu.geometry;

import lombok.val;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.util.ArrayList;
import java.util.List;

public class PointsGridGenerator {

    public static List<Point> generatePoints(CoordinateReferenceSystem localCRS,
                                             Geometry bounds,
                                             double stepMeters,
                                             double mainAngle) throws FactoryException, TransformException {

        val envelope = bounds.getEnvelopeInternal();
        val preparedBounds = GeometryUtils.preparedGeometryFactory.create(bounds);

        val globalToLocal = CRS.findMathTransform(DefaultGeographicCRS.WGS84, localCRS);
        val localEnvelopeGeometry = JTS.transform(GeometryUtils.geometryFactory.toGeometry(envelope), globalToLocal);
        val localEnvelope = localEnvelopeGeometry.getEnvelopeInternal();
        val envelopeWidth = Math.max(localEnvelope.getWidth(), localEnvelope.getHeight());
        val envelopeDiagonal = Math.sqrt(Math.pow(localEnvelope.getWidth(), 2.0) + Math.pow(localEnvelope.getHeight(), 2.0));

        List<Point> points = new ArrayList<>();
        val gc = new GeodeticCalculator();
        var point = GeometryUtils.makePoint(new Coordinate(envelope.getMinX(), envelope.getMinY()));

        double mainStepAngle = mainAngle;
        while (mainStepAngle < 0.0) {
            mainStepAngle += 90.0;
        }
        while (mainStepAngle > 90.0) {
            mainStepAngle-= 90.0;
        }

        var distance = 0.0;
        do {
            gc.setStartingGeographicPoint(point.getX(), point.getY());
            if (preparedBounds.contains(point)) {
                points.add(point);
            }

            Point intermediatePoint;
            int pointIdx;

            double[] directions = new double[] {90d, -90d};
            for(double direction : directions) {
                pointIdx = 1;
                do {
                    gc.setDirection(mainStepAngle + direction, stepMeters * pointIdx);
                    val destPoint = gc.getDestinationGeographicPoint();
                    intermediatePoint = GeometryUtils.makePoint(new Coordinate(destPoint.getX(), destPoint.getY()));

                    if (preparedBounds.contains(intermediatePoint)) {
                        points.add(intermediatePoint);
                    }
                    ++pointIdx;
                }
                while (stepMeters * pointIdx < envelopeWidth);
            }

            gc.setDirection(mainStepAngle, stepMeters);
            val destPoint = gc.getDestinationGeographicPoint();
            point = GeometryUtils.makePoint(new Coordinate(destPoint.getX(), destPoint.getY()));
            distance += stepMeters;

        } while (distance < envelopeDiagonal);

        return points;
    }

}
