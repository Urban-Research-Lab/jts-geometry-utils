package ru.itmo.idu.geometry;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static ru.itmo.idu.geometry.GeometryUtils.geometryFactory;

public class SafeOperationsTest {

    @Test
    public void safeIntersectsTest() throws IOException {
        Geometry geomIntersects1 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geomIntersects2 = readGeometryFromGeoJSON("polygonIntersects2.json");
        Geometry geomIntersects3 = readGeometryFromGeoJSON("polygonIntersects3.json");
        assertTrue(SafeOperations.safeIntersects(geomIntersects1, geomIntersects2));
        assertFalse(SafeOperations.safeIntersects(geomIntersects1, geomIntersects3));
    }

    @Test
    public void safeCoversTest() throws IOException {
        Geometry geomCovers1 = readGeometryFromGeoJSON("polygonCovers1.json");
        Geometry geomCovers2 = readGeometryFromGeoJSON("polygonCovers2.json");
        Geometry geomCovers3 = readGeometryFromGeoJSON("polygonCovers3.json");
        assertTrue(SafeOperations.safeCovers(geomCovers1, geomCovers2));
        assertFalse(SafeOperations.safeCovers(geomCovers1, geomCovers3));
    }

    @Test
    public void safeContainsTest() throws IOException {
        Geometry geomContains1 = readGeometryFromGeoJSON("polygonCovers1.json");
        Geometry geomContains2 = readGeometryFromGeoJSON("polygonCovers2.json");
        Geometry geomContains3 = readGeometryFromGeoJSON("polygonCovers3.json");
        assertTrue(SafeOperations.safeContains(geomContains1, geomContains2));
        assertFalse(SafeOperations.safeContains(geomContains1, geomContains3));
    }

    @Test
    public void safeIntersectionTest() throws IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Geometry geom2 = geometryFactory.createEmpty(2);
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        assertTrue(SafeOperations.safeIntersection(geom1, geom2).isEmpty());
        assertFalse(SafeOperations.safeIntersection(geom3, geom4).isEmpty());
    }

    @Test
    public void safeDifferenceTest() throws IOException {
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        assertFalse(SafeOperations.safeDifference(geom3, geom4).isEmpty());
    }

    @Test
    public void testSafeDifferenceGeometryCollections() throws IOException {
        FeatureCollection fc = new FeatureJSON().readFeatureCollection(
                getClass().getClassLoader().getResourceAsStream("geomCollectionIntersection.geojson"));

        Object[] features = fc.toArray();
        final Geometry firstPolygon = (Geometry) ((Feature) features[0]).getDefaultGeometryProperty().getValue();
        Geometry firstCollection = geometryFactory.createGeometryCollection(new Geometry[]{
                firstPolygon,
                (Geometry) ((Feature) features[1]).getDefaultGeometryProperty().getValue(),
        });
        Geometry secondCollection = geometryFactory.createGeometryCollection(new Geometry[]{
                (Geometry) ((Feature) features[2]).getDefaultGeometryProperty().getValue(),
                (Geometry) ((Feature) features[3]).getDefaultGeometryProperty().getValue(),
        });

        Geometry difference = SafeOperations.safeDifference(firstCollection, secondCollection);

        assertEquals(1, difference.getNumGeometries());
        assertEquals(ProjectionUtils.calcArea(difference), ProjectionUtils.calcArea(firstPolygon), 0.0001);
    }

    @Test
    public void safeUnionTest() throws IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Geometry geom2 = geometryFactory.createEmpty(2);
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        assertTrue(SafeOperations.safeUnion(geom1, geom2).isEmpty());
        assertFalse(SafeOperations.safeUnion(geom3, geom4).isEmpty());
    }

    @Test
    public void safeUnionWorksOnGeometryCollections() {
        Geometry first = ProjectionUtils.makePointBuffer(new Coordinate(10, 10), 10.0);
        Geometry second = ProjectionUtils.makePointBuffer(new Coordinate(11, 11), 10.0);
        Geometry third = ProjectionUtils.makePointBuffer(new Coordinate(12, 12), 10.0);

        Geometry firstCollection = geometryFactory.createGeometryCollection(new Geometry[]{first, second});
        Geometry secondCollection = geometryFactory.createGeometryCollection(new Geometry[]{third});

        Geometry result = SafeOperations.safeUnion(firstCollection, secondCollection);

        assertEquals(3, result.getNumGeometries());
    }

    protected Geometry readGeometryFromGeoJSON(String resourceName) throws IOException {
        FeatureCollection fc = new FeatureJSON().readFeatureCollection(getClass().getClassLoader().getResourceAsStream(resourceName));

        Feature next = fc.features().next();
        return (Geometry) next.getDefaultGeometryProperty().getValue();
    }
}
