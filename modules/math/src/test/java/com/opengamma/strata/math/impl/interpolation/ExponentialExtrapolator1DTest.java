/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test related to the exponential extrapolator.
 */
@Test
public class ExponentialExtrapolator1DTest {

  private static final Interpolator1D INTERPOLATOR = new LinearInterpolator1D();
  private static final ExponentialExtrapolator1D EXP_EXTRAPOLATOR = new ExponentialExtrapolator1D();

  private static final double[] X_DATA = new double[] {0.01, 1.0, 5.0, 10.0 };
  private static final double[] Y_DATA = new double[] {0.99, 0.98, 0.90, 0.80 };

  private static final Interpolator1DDataBundle DATA;
  static {
    DATA = INTERPOLATOR.getDataBundleFromSortedArrays(X_DATA, Y_DATA);
  }
  private static final double TOLERANCE_VALUE = 1.0E-10;
  private static final double TOLERANCE_SENSI = 1.0E-5;

  @Test
  public void value() {
    double mLeft = Math.log(Y_DATA[0]) / X_DATA[0];
    double mRight = Math.log(Y_DATA[X_DATA.length - 1]) / X_DATA[X_DATA.length - 1];
    double value;
    value = 0.0;
    assertEquals("ExponentialExtrapolator1D: value", 1.0, EXP_EXTRAPOLATOR.extrapolate(DATA, value, INTERPOLATOR), TOLERANCE_VALUE);
    value = -0.2;
    assertEquals("ExponentialExtrapolator1D: value", Math.exp(mLeft * value), EXP_EXTRAPOLATOR.extrapolate(DATA, value, INTERPOLATOR), TOLERANCE_VALUE);
    value = 11.0;
    assertEquals("ExponentialExtrapolator1D: value", Math.exp(mRight * value), EXP_EXTRAPOLATOR.extrapolate(DATA, value, INTERPOLATOR), TOLERANCE_VALUE);
  }

  @Test
  public void sensitivity() {
    double value;
    double shift = 1.0E-8;
    double[] yDataShifted;
    value = 0.0;
    yDataShifted = Y_DATA.clone();
    yDataShifted[0] += shift;
    assertEquals("ExponentialExtrapolator1D: value",
        (EXP_EXTRAPOLATOR.extrapolate(INTERPOLATOR.getDataBundleFromSortedArrays(X_DATA, yDataShifted), value, INTERPOLATOR) - EXP_EXTRAPOLATOR.extrapolate(DATA, value, INTERPOLATOR)) / shift,
        EXP_EXTRAPOLATOR.getNodeSensitivitiesForValue(DATA, value, INTERPOLATOR)[0], TOLERANCE_SENSI);
    value = -0.2;
    yDataShifted = Y_DATA.clone();
    yDataShifted[0] += shift;
    assertEquals("ExponentialExtrapolator1D: value",
        (EXP_EXTRAPOLATOR.extrapolate(INTERPOLATOR.getDataBundleFromSortedArrays(X_DATA, yDataShifted), value, INTERPOLATOR) - EXP_EXTRAPOLATOR.extrapolate(DATA, value, INTERPOLATOR)) / shift,
        EXP_EXTRAPOLATOR.getNodeSensitivitiesForValue(DATA, value, INTERPOLATOR)[0], TOLERANCE_SENSI);
    value = 11.0;
    yDataShifted = Y_DATA.clone();
    yDataShifted[Y_DATA.length - 1] += shift;
    assertEquals("ExponentialExtrapolator1D: value",
        (EXP_EXTRAPOLATOR.extrapolate(INTERPOLATOR.getDataBundleFromSortedArrays(X_DATA, yDataShifted), value, INTERPOLATOR) - EXP_EXTRAPOLATOR.extrapolate(DATA, value, INTERPOLATOR)) / shift,
        EXP_EXTRAPOLATOR.getNodeSensitivitiesForValue(DATA, value, INTERPOLATOR)[Y_DATA.length - 1], TOLERANCE_SENSI);
  }

}
