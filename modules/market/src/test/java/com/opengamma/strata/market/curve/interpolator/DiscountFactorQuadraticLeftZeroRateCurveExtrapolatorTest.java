/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.PRODUCT_LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link DiscountFactorQuadraticLeftZeroRateCurveExtrapolator}.
 */
@Test
public class DiscountFactorQuadraticLeftZeroRateCurveExtrapolatorTest {

  private static final DoubleArray X_DATA = DoubleArray.of(0.3, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final double EPS = 1.e-7;
  private static final double TOL = 1.e-11;

  public void basicsTest() {
    assertEquals(
        DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE.getName(),
        DiscountFactorQuadraticLeftZeroRateCurveExtrapolator.NAME);
    assertEquals(
        DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE.toString(),
        DiscountFactorQuadraticLeftZeroRateCurveExtrapolator.NAME);
  }

  public void sameIntervalsTest() {
    DoubleArray xValues = DoubleArray.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0);
    DoubleArray[] yValues = new DoubleArray[] {
        DoubleArray.of(0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001),
        DoubleArray.of(-11.0, 8.0, 5.0, 1.001, -1.001, 5.0, -8.0, 11.0),
        DoubleArray.of(0.001, -0.001, 5.0, 9.0, 9.0, 12.0, 18.0, 18.0) };
    int nData = xValues.size();
    int nKeys = 100 * nData;
    double[] xKeys = new double[nKeys];
    double xMin = 0.;
    double xMax = xValues.get(nData - 1) + 2d;
    double step = (xMax - xMin) / nKeys;
    for (int i = 0; i < nKeys; ++i) {
      xKeys[i] = xMin + step * i;
    }
    int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      BoundCurveInterpolator bci = PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
          xValues, yValues[k], DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE, PRODUCT_LINEAR);

      // Check C0 continuity
      assertEquals(
          bci.interpolate(xValues.get(0) - TOL),
          bci.interpolate(xValues.get(0)),
          TOL * 1.0e2);
      // Check C1 continuity
      assertEquals(bci.firstDerivative(xValues.get(0) - TOL),
          bci.firstDerivative(xValues.get(0)),
          Math.sqrt(TOL));
      // Test sensitivity
      double[] yValues1Up = yValues[k].toArray();
      double[] yValues1Dw = yValues[k].toArray();
      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues[k].get(j) * (1d + EPS);
        yValues1Dw[j] = yValues[k].get(j) * (1d - EPS);
        BoundCurveInterpolator bciUp = PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Up), DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE,
            PRODUCT_LINEAR);
        BoundCurveInterpolator bciDw = PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Dw), DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE,
            PRODUCT_LINEAR);
        for (int i = 2; i < nKeys; ++i) {
          double exp = 0.5 * (bciUp.interpolate(xKeys[i]) - bciDw.interpolate(xKeys[i])) / EPS /
              yValues[k].get(j);
            assertEquals(bci.parameterSensitivity(xKeys[i]).get(j), exp,
              Math.max(Math.abs(yValues[k].get(j)) * EPS, EPS) * 1e3);//because gradient is NOT exact  TODO
        }
        yValues1Up[j] = yValues[k].get(j);
        yValues1Dw[j] = yValues[k].get(j);
      }
    }
  }

  public void differentIntervalsTest() {
    DoubleArray xValues = DoubleArray.of(
        1.0328724558967068, 1.2692381049172323, 2.8611430465380905, 4.296118458251132,
        7.011992052151352, 7.293354144919639, 8.557971037612713, 8.77306861567384,
        10.572470371584489, 12.96945799507056);
    DoubleArray[] yValues = new DoubleArray[] {
        DoubleArray.of(0.11593075755231343, 0.2794957672828094, 0.4674733634811079,
            0.5517689918508841, 0.6138447304104604, 0.6264375977142906, 0.6581666492568779,
            0.8378685055774037, 0.10005246918325483, 0.10468304334744241),
        DoubleArray.of(0.995780079114617, 0.8733013195721913, 0.8192165283188197,
            0.6539369493529048, 0.63868683960757515, 0.4700471352238411, 0.4555354921077598,
            0.3780781869340659, 0.2299369456202763, 0.9182441378327986) };
    int nData = xValues.size();
    int nKeys = 100 * nData;
    double[] xKeys = new double[nKeys];
    double xMin = 0.;
    double xMax = xValues.get(nData - 1) + 2d;
    double step = (xMax - xMin) / nKeys;
    for (int i = 0; i < nKeys; ++i) {
      xKeys[i] = xMin + step * i;
    }
    int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      BoundCurveInterpolator bci = PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
          xValues, yValues[k], DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE, PRODUCT_LINEAR);

      // Check C0 continuity
      assertEquals(
          bci.interpolate(xValues.get(0) - TOL),
          bci.interpolate(xValues.get(0)),
          TOL * 1.0e2);
      // Check C1 continuity
      assertEquals(
          bci.firstDerivative(xValues.get(0) - TOL),
          bci.firstDerivative(xValues.get(0)),
          Math.sqrt(TOL));
      // Test sensitivity
      double[] yValues1Up = yValues[k].toArray();
      double[] yValues1Dw = yValues[k].toArray();
      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues[k].get(j) * (1d + EPS);
        yValues1Dw[j] = yValues[k].get(j) * (1d - EPS);
        BoundCurveInterpolator bciUp = PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Up), DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE,
            PRODUCT_LINEAR);
        BoundCurveInterpolator bciDw = PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Dw), DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE,
            PRODUCT_LINEAR);
        for (int i = 0; i < nKeys; ++i) {
          double res1 =
              0.5 * (bciUp.interpolate(xKeys[i]) - bciDw.interpolate(xKeys[i])) / EPS /
                  yValues[k].get(j);
          assertEquals(res1, bci.parameterSensitivity(xKeys[i]).get(j),
              Math.max(Math.abs(yValues[k].get(j)) * EPS, EPS) * 1e2);//because gradient is NOT exact
        }
        yValues1Up[j] = yValues[k].get(j);
        yValues1Dw[j] = yValues[k].get(j);
      }
    }
  }

  public void limitingTest() {
    BoundCurveInterpolator bci = LINEAR.bind(
        X_DATA, Y_DATA, DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE, PRODUCT_LINEAR);
    double small = 1.0e-8;
    assertEquals(bci.interpolate(small), bci.interpolate(10d * small), Y_DATA.get(0) * 10d * small);
    double derivative = bci.firstDerivative(small);
    double derivativeExp =
        (bci.interpolate(small + 0.5 * small) - bci.interpolate(small - 0.5 * small)) / small;
    assertEquals(derivative, derivativeExp, Y_DATA.get(0) * small);
    DoubleArray sensi = bci.parameterSensitivity(small);
    DoubleArray sensiS = bci.parameterSensitivity(small * 5d);
    assertTrue(DoubleArrayMath.fuzzyEquals(
        sensi.toArray(),
        sensiS.toArray(),
        Y_DATA.get(0) * 10d * small));
  }

  public void noRightTest() {
    BoundCurveInterpolator bci = LINEAR.bind(
        X_DATA,
        Y_DATA,
        DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE,
        DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE);
    assertThrowsIllegalArg(() -> bci.interpolate(10d));
    assertThrowsIllegalArg(() -> bci.firstDerivative(10d));
    assertThrowsIllegalArg(() -> bci.parameterSensitivity(10d));
  }

  //-------------------------------------------------------------------------
  public void serializationTest() {
    assertSerialization(DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE);
  }

}
