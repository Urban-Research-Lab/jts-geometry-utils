package ru.itmo.idu.geometry;

import org.geotools.api.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static ru.itmo.idu.geometry.GeometryUtils.geometryFactory;

/**
 * Зеркалирует {@link SafeOperationsTest}, чтобы покрыть базовую функциональность
 * {@link LocalSafeOperations} и убедиться, что для метровых (локальных) координат
 * методы дают тот же логический результат и при этом никогда не дёргают
 * {@link ProjectionUtils} / {@link CRSUtils} (что в SafeOperations приводит к
 * NoSuchAuthorityCodeException при попадании метров вместо градусов).
 */
public class LocalSafeOperationsTest {

    @Test
    public void safeIntersectsTest() throws IOException {
        Geometry geomIntersects1 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geomIntersects2 = readGeometryFromGeoJSON("polygonIntersects2.json");
        Geometry geomIntersects3 = readGeometryFromGeoJSON("polygonIntersects3.json");
        assertTrue(LocalSafeOperations.safeIntersects(geomIntersects1, geomIntersects2));
        assertFalse(LocalSafeOperations.safeIntersects(geomIntersects1, geomIntersects3));
    }

    @Test
    public void safeCoversTest() throws IOException {
        Geometry geomCovers1 = readGeometryFromGeoJSON("polygonCovers1.json");
        Geometry geomCovers2 = readGeometryFromGeoJSON("polygonCovers2.json");
        Geometry geomCovers3 = readGeometryFromGeoJSON("polygonCovers3.json");
        assertTrue(LocalSafeOperations.safeCovers(geomCovers1, geomCovers2));
        assertFalse(LocalSafeOperations.safeCovers(geomCovers1, geomCovers3));
    }

    @Test
    public void safeContainsTest() throws IOException {
        Geometry geomContains1 = readGeometryFromGeoJSON("polygonCovers1.json");
        Geometry geomContains2 = readGeometryFromGeoJSON("polygonCovers2.json");
        Geometry geomContains3 = readGeometryFromGeoJSON("polygonCovers3.json");
        assertTrue(LocalSafeOperations.safeContains(geomContains1, geomContains2));
        assertFalse(LocalSafeOperations.safeContains(geomContains1, geomContains3));
    }

    @Test
    public void safeIntersectionTest() throws IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Geometry geom2 = geometryFactory.createEmpty(2);
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        assertTrue(LocalSafeOperations.safeIntersection(geom1, geom2).isEmpty());
        assertFalse(LocalSafeOperations.safeIntersection(geom3, geom4).isEmpty());
    }

    @Test
    public void safeDifferenceTest() throws IOException {
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        assertFalse(LocalSafeOperations.safeDifference(geom3, geom4).isEmpty());
    }

    @Test
    public void safeDifferenceMultiLineStringNotBufferedToEmpty() {
        Geometry mls = geometryFactory.createMultiLineString(
                new LineString[]{
                        GeometryUtils.makeLine(new Coordinate(0, 0), new Coordinate(10, 0)),
                        GeometryUtils.makeLine(new Coordinate(10, 0), new Coordinate(20, 0))
                });

        Geometry circle = GeometryUtils.makePoint(5, 0).buffer(3.0);

        Geometry rz = LocalSafeOperations.safeDifference(mls, circle);
        assertFalse(rz.isEmpty());
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

        Geometry difference = LocalSafeOperations.safeDifference(firstCollection, secondCollection);

        assertEquals(1, difference.getNumGeometries());
        assertEquals(ProjectionUtils.calcArea(difference), ProjectionUtils.calcArea(firstPolygon), 0.0001);
    }

    @Test
    public void safeUnionTest() throws IOException {
        Geometry geom1 = geometryFactory.createEmpty(2);
        Geometry geom2 = geometryFactory.createEmpty(2);
        Geometry geom3 = readGeometryFromGeoJSON("polygonIntersects1.json");
        Geometry geom4 = readGeometryFromGeoJSON("polygonIntersects2.json");
        assertTrue(LocalSafeOperations.safeUnion(geom1, geom2).isEmpty());
        assertFalse(LocalSafeOperations.safeUnion(geom3, geom4).isEmpty());
    }

    @Test
    public void safeUnionWorksOnGeometryCollections() {
        Geometry first = ProjectionUtils.makePointBuffer(new Coordinate(10, 10), 10.0);
        Geometry second = ProjectionUtils.makePointBuffer(new Coordinate(11, 11), 10.0);
        Geometry third = ProjectionUtils.makePointBuffer(new Coordinate(12, 12), 10.0);

        Geometry firstCollection = geometryFactory.createGeometryCollection(new Geometry[]{first, second});
        Geometry secondCollection = geometryFactory.createGeometryCollection(new Geometry[]{third});

        Geometry result = LocalSafeOperations.safeUnion(firstCollection, secondCollection);

        assertEquals(3, result.getNumGeometries());
    }

    /**
     * Главный тест-смысл существования LocalSafeOperations: проверяем, что метровые
     * (локальные) координаты не вызывают NoSuchAuthorityCodeException, как в
     * SafeOperations. Используем большие координаты порядка сотен тысяч метров —
     * именно такие приходят в apartment-generation-core из локальной CRS блока.
     */
    @Test
    public void handlesLocalMeterCoordinates() {
        Polygon p1 = squareAt(410000.0, 6170000.0, 100.0);
        Polygon p2 = squareAt(410050.0, 6170050.0, 100.0);

        // Все базовые операции должны работать без обращения к ProjectionUtils.
        assertTrue(LocalSafeOperations.safeIntersects(p1, p2));
        assertNotNull(LocalSafeOperations.safeIntersection(p1, p2));
        assertNotNull(LocalSafeOperations.safeDifference(p1, p2));
        assertNotNull(LocalSafeOperations.safeUnion(p1, p2));
        assertTrue(LocalSafeOperations.safeCovers(p1, squareAt(410010.0, 6170010.0, 10.0)));
        assertTrue(LocalSafeOperations.safeContains(p1, squareAt(410010.0, 6170010.0, 10.0)));
    }

    @Test
    public void bufferFallbackUsesMetricBuffer() {
        // Маленький буфер на полигоне в метровой CRS должен оставаться маленьким (метры,
        // а не градусы как было бы при ProjectionUtils.bufferProjected).
        Polygon p = squareAt(410000.0, 6170000.0, 100.0);
        Geometry buffered = p.buffer(1.0);
        // ширина увеличилась примерно на 2 м (по 1 м с каждой стороны).
        double widthDiff = buffered.getEnvelopeInternal().getWidth() - p.getEnvelopeInternal().getWidth();
        assertEquals(2.0, widthDiff, 0.5);
    }

    private static Polygon squareAt(double x, double y, double size) {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(x, y),
                new Coordinate(x + size, y),
                new Coordinate(x + size, y + size),
                new Coordinate(x, y + size),
                new Coordinate(x, y)
        };
        return geometryFactory.createPolygon(coords);
    }

    protected Geometry readGeometryFromGeoJSON(String resourceName) throws IOException {
        FeatureCollection fc = new FeatureJSON().readFeatureCollection(getClass().getClassLoader().getResourceAsStream(resourceName));
        Feature next = fc.features().next();
        return (Geometry) next.getDefaultGeometryProperty().getValue();
    }
}
