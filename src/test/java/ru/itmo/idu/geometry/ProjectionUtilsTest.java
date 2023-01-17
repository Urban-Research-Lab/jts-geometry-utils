package ru.itmo.idu.geometry;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

import static ru.itmo.idu.geometry.GeometryUtils.geometryFactory;

public class ProjectionUtilsTest {

    @Test
    public void transformToLocalCRSTest() throws FactoryException, TransformException, IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Assert.assertTrue(ProjectionUtils.transformToLocalCRS(geom1).isEmpty());
        CoordinateReferenceSystem localCrs = CRSUtils.getLocalCRS(readGeometryFromGeoJSON("polygonIntersects1.json"));
        Assert.assertTrue(ProjectionUtils.transformToLocalCRS(localCrs, geom1).isEmpty());
        Assert.assertTrue(ProjectionUtils.transformFromLocalCRS(localCrs, geom1).isEmpty());
        Assert.assertTrue(ProjectionUtils.transformFromLocalCRS(geom1).isEmpty());
    }

    @Test
    public void calcAreaTest() {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Assert.assertEquals(0.0, ProjectionUtils.calcArea(geom1), 0.01);
    }

    @Test
    public void bufferProjectedTest() throws IOException, FactoryException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Assert.assertTrue(ProjectionUtils.bufferProjected(geom1, 2.0).isEmpty());
        CoordinateReferenceSystem localCrs = CRSUtils.getLocalCRS(readGeometryFromGeoJSON("polygonIntersects1.json"));
        Assert.assertTrue(ProjectionUtils.bufferProjected(localCrs, geom1, 2.0).isEmpty());
        Assert.assertTrue(ProjectionUtils.simplifyProjected(geom1, 2.0).isEmpty());
    }

    protected Geometry readGeometryFromGeoJSON(String resourceName) throws IOException {
        FeatureCollection fc = new FeatureJSON().readFeatureCollection(getClass().getClassLoader().getResourceAsStream(resourceName));

        Feature next = fc.features().next();
        return (Geometry) next.getDefaultGeometryProperty().getValue();
    }
}
