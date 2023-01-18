package ru.itmo.idu.geometry;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.Feature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
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
}
