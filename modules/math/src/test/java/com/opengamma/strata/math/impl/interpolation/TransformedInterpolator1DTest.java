/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.minimization.DoubleRangeLimitTransform;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform;

/**
 * Test.
 */
@Test
public class TransformedInterpolator1DTest {

  private static final Interpolator1D INTERPOLATOR_BASE = new DoubleQuadraticInterpolator1D();
  private static final ParameterLimitsTransform TRANSFORM = new DoubleRangeLimitTransform(0, 1);
  private static final Interpolator1D INTERPOLATOR = new TransformedInterpolator1D(INTERPOLATOR_BASE, TRANSFORM);

  private static final double[] X_DATA = new double[] {0, 0.4, 1.0, 1.5, 2.2, 4.8, 5.0 };
  private static final double[] Y_DATA = new double[] {.1, .95, .05, .05, .9, 0.2, 0.1 };
  private static final double[] YSTAR_DATA;
  private static final Interpolator1DDataBundle TRANS_DATA_BUNDLE;

  static {
    int n = Y_DATA.length;
    YSTAR_DATA = new double[n];
    for (int i = 0; i < n; i++) {
      YSTAR_DATA[i] = TRANSFORM.transform(Y_DATA[i]);
    }
    TRANS_DATA_BUNDLE = INTERPOLATOR.getDataBundleFromSortedArrays(X_DATA, YSTAR_DATA);
  }

  public void testCorrectAtNodes() {
    final int n = Y_DATA.length;
    for (int i = 0; i < n; i++) {
      double y = INTERPOLATOR.interpolate(TRANS_DATA_BUNDLE, X_DATA[i]);
      assertEquals(Y_DATA[i], y, 1e-12);
    }
  }

  public void testInRange() {
    for (int i = 0; i < 200; i++) {
      double x = 5.0 * i / 199.0;
      double y = INTERPOLATOR.interpolate(TRANS_DATA_BUNDLE, x);
      assertTrue(y >= 0 && y <= 1);
    }
  }

  public void testSensitivity() {
    for (int i = 0; i < 20; i++) {
      double x = 5.0 * i / 19.0;
      double[] fdSense = INTERPOLATOR.getFiniteDifferenceSensitivities(TRANS_DATA_BUNDLE, x);
      double[] analSense = INTERPOLATOR.getNodeSensitivitiesForValue(TRANS_DATA_BUNDLE, x);
      for (int j = 0; j < fdSense.length; j++) {
        assertEquals(analSense[j], fdSense[j], 1e-8);
      }
    }

  }

}
