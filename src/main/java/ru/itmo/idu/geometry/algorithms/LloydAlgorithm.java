package ru.itmo.idu.geometry.algorithms;

import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import ru.itmo.idu.geometry.GeometryUtils;
import ru.itmo.idu.geometry.ProjectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lloyds algorithm fills polygon with given amount of points, distributing them almost equally at even distances
 * from each other. But this method is quite imprecise, so distance between nearby points may vary about 10% of provided value
 * This does not form any regular (rectangular or hex) grid
 *
 * Starting from initial random points distribution, on each step generates Voronoi diagram, then shifts points to
 * centroids of Voronoi diagram cells. Repeats until process converges.
 *
 * @link https://en.wikipedia.org/wiki/Lloyd%27s_algorithm
 */
public class LloydAlgorithm {

    private final Geometry area;

    private final double pointDistance;

    private MultiPoint currentPoints;

    private List<Coordinate> result;

    @Setter
    private int maxIterations = 50;

    /**
     * Will generate points filling this polygonal area with points
     * Points will have average given distance from each other
     * Here area is in local metric CRS. Use static factory method generateLloydPointsWGS84() for geometries in WGS84,
     * or convert it manually
     * pointDistance is in units of area CRS (meters)
     */
    public LloydAlgorithm(Geometry area, double pointDistance) {
        this.area = area;
        this.pointDistance = pointDistance;
        if (pointDistance <= 0) {
            throw new IllegalArgumentException("pointDistance shall be positive");
        }
    }

    private MultiPoint run() {
        for (int iteration = 0; iteration < maxIterations; ++iteration) {
            var vdb = new VoronoiDiagramBuilder();
            vdb.setSites(currentPoints);
            var vd = vdb.getDiagram(GeometryUtils.geometryFactory);
            var centroids = calculateCentroids(vd);
            currentPoints = createMultiPoint(centroids);
        }
        return currentPoints;
    }

    private List<Coordinate> calculateCentroids(Geometry clusters) {
        var centroids = new ArrayList<Coordinate>();
        for (int i = 0; i < clusters.getNumGeometries(); ++i) {
            var diagramCell = clusters.getGeometryN(i);
            var clampedDiagramCell = diagramCell.intersection(area);
            centroids.add(clampedDiagramCell.getCentroid().getCoordinate());
        }
        return centroids;
    }

    private MultiPoint createMultiPoint(List<Coordinate> coordinates) {
        var points = coordinates.stream().map(point -> area.getFactory().createPoint(point) ).collect(Collectors.toList());
        return GeometryUtils.geometryFactory.createMultiPoint(points.toArray(Point[]::new));
    }

    /**
     * Generates coordinates of evenly distributed points within given geometry =
     */
    public Coordinate[] generateLloydPoints() {
        if (area.isEmpty()) {
            return new Coordinate[0];
        }
        // start with random points inside our area, point amount depends on desired distance between them and area square
        var pointsCount = (int)Math.ceil((area.getArea() / (Math.pow(pointDistance, 2.0))));
        var rpb = new RandomPointsBuilder();
        rpb.setNumPoints(pointsCount);
        rpb.setExtent(area);
        currentPoints = (MultiPoint) rpb.getGeometry();
        var rzLocal = run();
        return rzLocal.getCoordinates();
    }

    /**
     * Generates coordinates of evenly distributed points within given geometry in WGS84
     * Helper method for wrapping LloydAlgorithm for geometries with lat-lon coordinates
     */
    public static Coordinate[] generateLloydPointsWGS84(CoordinateReferenceSystem crs,
                                                        Geometry area,
                                                        double metersBetweenPoints) throws FactoryException, TransformException {
        if (area.isEmpty()) {
            return new Coordinate[0];
        }
        var localArea = ProjectionUtils.transformToLocalCRS(crs, area);
        var algo = new LloydAlgorithm(localArea, metersBetweenPoints);
        Coordinate[] localCoords = algo.generateLloydPoints();
        return Arrays.stream(localCoords).map(coord -> ProjectionUtils.transformFromLocalCRS(crs, coord)).toArray(Coordinate[]::new);
    }
}
