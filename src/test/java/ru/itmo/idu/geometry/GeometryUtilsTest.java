package ru.itmo.idu.geometry;

import org.junit.Assert;
import org.junit.Test;

public class GeometryUtilsTest {

    @Test
    public void testAngleToAzimuth() {
        double azimuth = -2;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        Assert.assertEquals(358, azimuth, 0.001);

        azimuth = -362;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        Assert.assertEquals(358, azimuth, 0.001);

        azimuth = 0;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        Assert.assertEquals(0, azimuth, 0.001);

        azimuth = 360;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        Assert.assertEquals(0, azimuth, 0.001);

        azimuth = 2;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        Assert.assertEquals(2, azimuth, 0.001);

        azimuth = 362;
        azimuth = GeometryUtils.fixAzimuth(azimuth);
        Assert.assertEquals(2, azimuth, 0.001);
    }
}