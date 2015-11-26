/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DPiecewisePoynomialDataBundle;

/**
 * Test.
 */
@Test
public class PCHIPInterpolator1DTest {

  private static final Interpolator1D INTERPOLATOR = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.PCHIP);

  private static final double[] X_DATA = new double[] {0, 0.4, 1.0, 1.8, 2.8, 5 };
  private static final double[] Y_DATA = new double[] {3., 4., 4.1, 4.5, 7.2, 8.0 };

  private static final Interpolator1DDataBundle DATA = INTERPOLATOR.getDataBundle(X_DATA, Y_DATA);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    INTERPOLATOR.interpolate(null, 2.3);
  }

  public void testDataBundleType1() {
    assertEquals(INTERPOLATOR.getDataBundle(X_DATA, Y_DATA).getClass(), Interpolator1DPiecewisePoynomialDataBundle.class);
  }

  public void testDataBundleType2() {
    assertEquals(INTERPOLATOR.getDataBundleFromSortedArrays(X_DATA, Y_DATA).getClass(), Interpolator1DPiecewisePoynomialDataBundle.class);
  }

  public void dataBundleTest() {
    Interpolator1DDataBundle db = INTERPOLATOR.getDataBundle(X_DATA, Y_DATA);
    double[] keys = db.getKeys();
    double[] values = db.getValues();
    final int n = X_DATA.length;
    assertEquals("keys length", n, keys.length);
    assertEquals("values length", n, values.length);
    for (int i = 0; i < n; i++) {
      assertEquals("keys " + i, X_DATA[i], keys[i]);
      assertEquals("values " + i, Y_DATA[i], values[i]);
    }
  }

  public void montonicTest() {
    final int n = 100;
    final double low = X_DATA[0];
    final double range = X_DATA[X_DATA.length - 1] - X_DATA[0];
    double value = INTERPOLATOR.interpolate(DATA, low);
    for (int i = 1; i < n; i++) {
      double x = low + i * range / (n - 1);
      double y = INTERPOLATOR.interpolate(DATA, x);
      assertTrue(y > value);
      value = y;
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void samexNodesTest() {
    final double[] xData = new double[] {0.4, 0.7, 0.9, 0.9, 1.3, 1.8 };
    final double[] yData = new double[] {0.4, 0.5, 0.6, 0.7, 0.8, 1.0 };
    final Interpolator1DDataBundle data = INTERPOLATOR.getDataBundle(xData, yData);
    double y = INTERPOLATOR.interpolate(data, 1.0);
    assertTrue("y: " + y, !Double.isNaN(y));
  }

}
