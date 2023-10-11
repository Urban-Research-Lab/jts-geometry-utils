# jts-geometry-utils

[![Java CI with Gradle](https://github.com/Urban-Research-Lab/jts-geometry-utils/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/Urban-Research-Lab/jts-geometry-utils/actions/workflows/gradle.yml)

Collection of helper classes for common geometry operations needed in GIS development. 
Works together with [JTS Topolgy Suite](https://github.com/locationtech/jts) and [Geotools](https://geotools.org) packages

Contains classes for buffering, intersection and proximity checks that operate on WGS84 geometry, as well as some other helpful algorithms.

## Motivation

Working with geometry in WGS84 (which is a common case for different GIS tasks) is really hard. You can not just use simple geometric operations like buffer, because in lat-lon CRS X and 
Y values are not the same. E.g. at Saint-Petersburg 60N latitude one degree of latitude is about 2 times larger (in meters) than 1 degree of longitude.

If you try to buffer WGS 84 geometry using JTS buffer method, you will receive a green oval, while you wish a yellow circle:

![2 buffers](/doc/2buffers.jpg)

Same problem happens with any other JTS method like finding closest points between geometries or calculating line lengths. 

In order for such algorithms to work correctly, you need to:
1. Project WGS84 geometry to some other CRS, that uses meters and has X and Y values in same units
2. Perform algorithm
3. Project its results back to your original WGS84

jts-geometry-utils library does this for you, providing simple interface where you only need to provide you WGS84 geometry

## Contents

This library consists of 3 main parts:

1. **JTS helpers**. Some common tasks, like creating polygons from lists of coordinates, require a lot of boilerplate JTS code.
We have classes that simplify that tasks and add some more convenient interfaces to JTS classes.
2. **WGS84 and CRS helpers**. As mentioned above, JTS geometry operations can not be applied directly to WGS84 geometry. This library
provides methods that do all required projection and CRS handling stuff inside, transparent to the client.
3. **Algorithms**. Mostly needed by our laboratory tasks of automated design and GIS AI, but maybe you will find something
useful here as well.

Main classes are:

1`CRSUtils` - methods for retrieving the most precise local CRS for a geometry in WGS84. This local CRS can be then
   used for precise area calculation or other tasks that require coordinates to be in meters, not in degrees.
2`GeometryUtils` - helper methods for creating and managing JTS geometry objects.
3`InvertCoordinateFilter` - class that swaps X and Y coordinates. Is really helpful as some tools and some CRS specify lat-lon order
   while others use lon-lat
4`ProjectionUtils` - methods for projecting different types of geometries to another coordinate system. Common geometry operations, like
   buffer, can not be done in lat-lon coords (as 1 degree of latitude and longitude has different length in meters on different points on Earth surface).
   This class contains wrappers that first project geometries to local CRS, do the operation and then reproject result back to WGS84 if necessary.
5`SafeOperations` - methods that try not to throw TopologyException. They copy Geometry methods like `intersects`, `contains` etc.,
but also validate geometry and if operation still fails - try to repeat it with a slightly buffered version, to prevent annoying TPEs
6`Algorithms` - some complicated algorithms created for specific tasks. You probably wont need theese

## Usage

jts-geometry-utils is available at GitHub package registry. See instructions on
adding this registry to your project in [GitHub docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry).

For Gradle Kotlin add these lines to your repositories block:

```kotlin
  maven {
        url = uri("https://maven.pkg.github.com/Urban-Research-Lab/jts-geometry-utils")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
```
This code requires a GitHub account, and its credentials must be available as project or system properties. This 
is a problem with GitHub Gradle registry - you can not use it without GitHub account, even for public repos.

After you have set up your GitHub access, add this to your dependencies block:

```kotlin
implementation("ru.itmo.idu:jts-geometry-utils:3.0.0")
```

## Examples

Calculating area of a WGS84 geometry in square meters:

```java
Geometry geom = ...;
// this code will project geom to some most suitable local CRS and will calculate its area in that CRS
double areaInMeters = ProjectionUtils.calcArea(geom);
```

---
Making a 100 meters circle around a point at given lat-lon coords:

```java
Coordinate coordinate = new Coordinate(30.541368, 59.882656);

// helper for making Point from Coordinate
Point point = GeometryUtils.makePoint(coordinate); 

// this code will project point to local CRS, apply buffer and reproject back to WGS84
Geometry circle = ProjectionUtils.bufferProjected(point, 100.0);
```
---

Simplifying geometry, merging points that are closer than 5 meters from each other
```java
Geometry geom = ...;
// Converts to local CRS, uses JTS Simplifier class on that local geometry, then projects back to WGS84
Geometry simplified = ProjectionUtils.simplifyProjected(geom, 5.0);
```



## License

Code is licensed under terms of MIT license.