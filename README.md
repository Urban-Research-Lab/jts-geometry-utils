# jts-geometry-utils

Collection of helper classes for common geometry operations needed in GIS development

Contains following classes:

1. Algorithms.java - some complicated algorithms created for specific tasks. You probably wont need theese
2. CRSUtils.java - methods for retrieving the most precise local CRS for a geometry in WGS84. This local CRS can be then 
used for precise area calculation or other tasks that require coordinates to be in meters, not in degrees.
3. GeometryUtils.java - helper methods for creating and managing JTS geometry objects.
4. InvertCoordinateFilter - class that swaps X and Y coordinates. Is really helpful as some tools and some CRS specify lat-lon order
while others use lon-lat
5. ProjectionUtils - methods for projecting different types of geometries to another coordinate system. Common geometry operations, like 
buffer, can not be done in lat-lon coords (as 1 degree of latitude and longitude has different length in meters on different points on Earth surface). 
This class contains wrappers that first project geometries to local CRS, do the operation and then reproject result back to WGS84 if necessary.