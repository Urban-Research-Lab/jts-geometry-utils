package ru.itmo.idu.geometry.algorithms;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import ru.itmo.idu.geometry.GeometryUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LineDensifierTest {

    private static final double EPSILON = 1e-9;

    @Test
    public void testDensifyLineString_NormalCase() {
        Coordinate[] coords = {
            new Coordinate(0, 0),
            new Coordinate(0, 10),
            new Coordinate(10, 10)
        };
        LineString line = GeometryUtils.makeLine(coords);
        
        LineString result = LineDensifier.densifyLineString(line, 3.0);
        
        // Check that no segment is longer than maxPointDistance
        Coordinate[] resultCoords = result.getCoordinates();
        for (int i = 1; i < resultCoords.length; i++) {
            double segmentLength = resultCoords[i-1].distance(resultCoords[i]);
            assertTrue(segmentLength <= 3.0 + EPSILON, 
                "Segment from " + resultCoords[i-1] + " to " + resultCoords[i] + 
                " is longer than maxPointDistance: " + segmentLength);
        }
        
        // Verify the start and end points are preserved
        assertEquals(0, resultCoords[0].x, EPSILON);
        assertEquals(0, resultCoords[0].y, EPSILON);
        assertEquals(10, resultCoords[resultCoords.length-1].x, EPSILON);
        assertEquals(10, resultCoords[resultCoords.length-1].y, EPSILON);
    }

    @Test
    public void testDensifyLineString_EmptyLine() {
        LineString emptyLine = GeometryUtils.makeEmptyLine();
        LineString result = LineDensifier.densifyLineString(emptyLine, 1.0);
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDensifyLineString_TwoPoints_NoDensificationNeeded() {
        Coordinate[] coords = {
            new Coordinate(0, 0),
            new Coordinate(1, 1)
        };
        LineString line = GeometryUtils.makeLine(coords);
        
        LineString result = LineDensifier.densifyLineString(line, 2.0); // Distance ~1.41 < 2.0
        
        assertEquals(2, result.getNumPoints());
        assertEquals(0, result.getCoordinateN(0).x, EPSILON);
        assertEquals(0, result.getCoordinateN(0).y, EPSILON);
        assertEquals(1, result.getCoordinateN(1).x, EPSILON);
        assertEquals(1, result.getCoordinateN(1).y, EPSILON);
    }

    @Test
    public void testDensifyLineString_InvalidMaxDistance() {
        Coordinate[] coords = {
            new Coordinate(0, 0),
            new Coordinate(1, 1)
        };
        LineString line = GeometryUtils.makeLine(coords);
        
        assertThrows(IllegalArgumentException.class, () -> {
            LineDensifier.densifyLineString(line, -1.0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            LineDensifier.densifyLineString(line, 0.0);
        });
    }

    @Test
    public void testDensifyLineString_NoDuplicateCoordinates() {
        Coordinate[] coords = {
            new Coordinate(0, 0),
            new Coordinate(0, 5),
            new Coordinate(5, 5)
        };
        LineString line = GeometryUtils.makeLine(coords);
        
        LineString result = LineDensifier.densifyLineString(line, 2.0);
        
        // Check for duplicate consecutive coordinates
        Coordinate[] resultCoords = result.getCoordinates();
        Set<String> coordinatePairs = new HashSet<>();
        
        for (int i = 0; i < resultCoords.length - 1; i++) {
            String pairKey = String.format("%.9f,%.9f -> %.9f,%.9f", 
                resultCoords[i].x, resultCoords[i].y,
                resultCoords[i+1].x, resultCoords[i+1].y);
            
            assertFalse(coordinatePairs.contains(pairKey), 
                "Duplicate segment found: " + pairKey);
            coordinatePairs.add(pairKey);
        }
        
        // Also check that no consecutive points have identical coordinates
        for (int i = 1; i < resultCoords.length; i++) {
            assertNotEquals(resultCoords[i - 1], resultCoords[i], "Consecutive duplicate coordinates found at position " + (i - 1) + ": " + resultCoords[i]);
        }
    }

    @Test
    public void testDensifyLineSegment_NormalCase() {
        LineSegment segment = new LineSegment(
            new Coordinate(0, 0),
            new Coordinate(0, 8)
        );
        
        LineString result = LineDensifier.densifyLineSegment(segment, 3.0);
        
        // Check that no segment is longer than maxPointDistance
        Coordinate[] resultCoords = result.getCoordinates();
        for (int i = 1; i < resultCoords.length; i++) {
            double segmentLength = resultCoords[i-1].distance(resultCoords[i]);
            assertTrue(segmentLength <= 3.0 + EPSILON, 
                "Segment from " + resultCoords[i-1] + " to " + resultCoords[i] + 
                " is longer than maxPointDistance: " + segmentLength);
        }
        
        // Verify the start and end points are preserved
        assertEquals(0, resultCoords[0].x, EPSILON);
        assertEquals(0, resultCoords[0].y, EPSILON);
        assertEquals(0, resultCoords[resultCoords.length-1].x, EPSILON);
        assertEquals(8, resultCoords[resultCoords.length-1].y, EPSILON);
    }

    @Test
    public void testDensifyLineSegment_SameStartEndPoints() {
        Coordinate point = new Coordinate(5, 5);
        LineSegment segment = new LineSegment(point, point);
        
        LineString result = LineDensifier.densifyLineSegment(segment, 1.0);
        
        assertEquals(0, result.getLength(), EPSILON);
        assertEquals(5, result.getCoordinateN(0).x, EPSILON);
        assertEquals(5, result.getCoordinateN(0).y, EPSILON);
    }

    @Test
    public void testDensifyLineSegment_InvalidMaxDistance() {
        LineSegment segment = new LineSegment(
            new Coordinate(0, 0),
            new Coordinate(1, 1)
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            LineDensifier.densifyLineSegment(segment, -1.0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            LineDensifier.densifyLineSegment(segment, 0.0);
        });
    }

    @Test
    public void testDensifyLineSegment_NoDuplicateCoordinates() {
        LineSegment segment = new LineSegment(
            new Coordinate(0, 0),
            new Coordinate(0, 6)
        );
        
        LineString result = LineDensifier.densifyLineSegment(segment, 2.5);
        
        // Check that no consecutive points have identical coordinates
        Coordinate[] resultCoords = result.getCoordinates();
        for (int i = 1; i < resultCoords.length; i++) {
            assertNotEquals(resultCoords[i - 1], resultCoords[i], "Consecutive duplicate coordinates found at position " + (i - 1) + ": " + resultCoords[i]);
        }
        
        // Verify the densification created the expected number of points
        // Distance is 6, max distance 2.5, so we need ceil(6/2.5) = 3 segments, 4 points total
        assertEquals(4, resultCoords.length);
    }
}