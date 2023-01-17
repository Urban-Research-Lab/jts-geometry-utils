package ru.itmo.idu.geometry;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;

import java.io.IOException;

import static ru.itmo.idu.geometry.GeometryUtils.geometryFactory;

public class SafeOperationsTest {

    @Test
    public void safeIntersectsTest() throws IOException {
        Geometry geomIntersects1 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geomIntersects2 = readGeometryFromGeoJSON("polygonIntersects2.json");
        Geometry geomIntersects3 = readGeometryFromGeoJSON("polygonIntersects3.json");
        Assert.assertTrue(SafeOperations.safeIntersects(geomIntersects1, geomIntersects2));
        Assert.assertFalse(SafeOperations.safeIntersects(geomIntersects1, geomIntersects3));
    }

    @Test
    public void safeCoversTest() throws IOException {
        Geometry geomCovers1 = readGeometryFromGeoJSON("polygonCovers1.json");
        Geometry geomCovers2 = readGeometryFromGeoJSON("polygonCovers2.json");
        Geometry geomCovers3 = readGeometryFromGeoJSON("polygonCovers3.json");
        Assert.assertTrue(SafeOperations.safeCovers(geomCovers1, geomCovers2));
        Assert.assertFalse(SafeOperations.safeCovers(geomCovers1, geomCovers3));
    }

    @Test
    public void safeContainsTest() throws IOException {
        Geometry geomContains1 = readGeometryFromGeoJSON("polygonCovers1.json");
        Geometry geomContains2 = readGeometryFromGeoJSON("polygonCovers2.json");
        Geometry geomContains3 = readGeometryFromGeoJSON("polygonCovers3.json");
        Assert.assertTrue(SafeOperations.safeContains(geomContains1, geomContains2));
        Assert.assertFalse(SafeOperations.safeContains(geomContains1, geomContains3));
    }

    @Test
    public void safeIntersectionTest() throws IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Geometry geom2 = geometryFactory.createEmpty(2);
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        Assert.assertTrue(SafeOperations.safeIntersection(geom1, geom2).isEmpty());
        Assert.assertFalse(SafeOperations.safeIntersection(geom3, geom4).isEmpty());
    }

    @Test
    public void safeDifferenceTest() throws IOException {
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        Assert.assertFalse(SafeOperations.safeDifference(geom3, geom4).isEmpty());
    }

    @Test
    public void safeUnionTest() throws IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Geometry geom2 = geometryFactory.createEmpty(2);
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        Assert.assertTrue(SafeOperations.safeUnion(geom1, geom2).isEmpty());
        Assert.assertFalse(SafeOperations.safeUnion(geom3, geom4).isEmpty());
    }

    protected Geometry readGeometryFromGeoJSON(String resourceName) throws IOException {
        FeatureCollection fc = new FeatureJSON().readFeatureCollection(getClass().getClassLoader().getResourceAsStream(resourceName));

        Feature next = fc.features().next();
        return (Geometry) next.getDefaultGeometryProperty().getValue();
    }
}
