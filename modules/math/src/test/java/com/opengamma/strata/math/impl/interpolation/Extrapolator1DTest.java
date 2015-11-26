/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDoubleQuadraticDataBundle;

/**
 * Test.
 */
@Test
public class Extrapolator1DTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final Interpolator1D INTERPOLATOR = new DoubleQuadraticInterpolator1D();
  private static final LinearExtrapolator1D LINEAR_EXTRAPOLATOR = new LinearExtrapolator1D();
  private static final FlatExtrapolator1D FLAT_EXTRAPOLATOR = new FlatExtrapolator1D();
  private static final Interpolator1DDataBundle DATA;

  private static final double[] X_DATA = new double[] {0, 0.4, 1.0, 1.8, 2.8, 5 };
  private static final double[] Y_DATA = new double[] {3., 4., 3.1, 2., 7., 2. };

  private static final double[] X_TEST = new double[] {-1.0, 6.0 };
  private static final double[] Y_TEST = new double[] {-1.1, -5.272727273 };

  static {
    DATA = INTERPOLATOR.getDataBundleFromSortedArrays(X_DATA, Y_DATA);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData1() {
    LINEAR_EXTRAPOLATOR.extrapolate(null, 1.4, INTERPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData2() {
    FLAT_EXTRAPOLATOR.extrapolate(null, 1.4, INTERPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueInRange1() {
    FLAT_EXTRAPOLATOR.extrapolate(DATA, 1.2, INTERPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueInRange2() {
    LINEAR_EXTRAPOLATOR.extrapolate(DATA, 1.2, INTERPOLATOR);
  }

  @Test
  public void testDataBundleType3() {
    assertEquals(INTERPOLATOR.getDataBundle(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), Interpolator1DDoubleQuadraticDataBundle.class);
  }

  @Test
  public void testDataBundleType4() {
    assertEquals(INTERPOLATOR.getDataBundleFromSortedArrays(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), Interpolator1DDoubleQuadraticDataBundle.class);
  }

  @Test
  public void testFlatExtrapolation() {
    for (int i = 0; i < 100; i++) {
      final double x = RANDOM.nextDouble() * 20.0 - 10;
      if (x < 0) {
        assertEquals(3.0, FLAT_EXTRAPOLATOR.extrapolate(DATA, x, INTERPOLATOR), 1e-12);
      } else if (x > 5.0) {
        assertEquals(2.0, FLAT_EXTRAPOLATOR.extrapolate(DATA, x, INTERPOLATOR), 1e-12);
      }
    }
  }

  @Test
  public void testLinearExtrapolation() {
    for (int i = 0; i < X_TEST.length; i++) {
      assertEquals(Y_TEST[i], LINEAR_EXTRAPOLATOR.extrapolate(DATA, X_TEST[i], INTERPOLATOR), 1e-6);
    }
  }

}
