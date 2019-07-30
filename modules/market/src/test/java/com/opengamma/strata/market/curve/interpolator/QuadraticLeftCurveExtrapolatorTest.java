/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link QuadraticLeftCurveExtrapolator}.
 */
@Test
public class QuadraticLeftCurveExtrapolatorTest {

  private static final CurveExtrapolator QL_EXTRAPOLATOR = QuadraticLeftCurveExtrapolator.INSTANCE;

  private static final DoubleArray X_DATA = DoubleArray.of(0.3, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);

  private static final double EPS = 1.e-7;
  private static final double TOL = 1.e-12;

  public void test_basics() {
    assertEquals(QL_EXTRAPOLATOR.getName(), QuadraticLeftCurveExtrapolator.NAME);
    assertEquals(QL_EXTRAPOLATOR.toString(), QuadraticLeftCurveExtrapolator.NAME);
  }

  public void sameIntervalsTest() {
    DoubleArray xValues = DoubleArray.of(1., 2., 3., 4., 5., 6., 7., 8.);
    DoubleArray[] yValues = new DoubleArray[] {
        DoubleArray.of(1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001),
        DoubleArray.of(11., 8., 5., 1.001, 1.001, 5., 8., 11.),
        DoubleArray.of(1.001, 1.001, 5., 9., 9., 12., 18., 18.)
    };
    int nData = xValues.size();
    int nKeys = 100 * nData;
    double[] xKeys = new double[nKeys];
    double xMin = 0.;
    double xMax = xValues.get(nData - 1) + 2.;
    double step = (xMax - xMin) / nKeys;
    for (int i = 0; i < nKeys; ++i) {
      xKeys[i] = xMin + step * i;
    }

    CurveExtrapolator extrap = QuadraticLeftCurveExtrapolator.INSTANCE;

    int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      BoundCurveInterpolator bci = CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
          xValues, yValues[k], extrap, CurveExtrapolators.LOG_LINEAR);

      // Check C0 continuity
      assertEquals(bci.interpolate(xValues.get(0) - 1.e-14), bci.interpolate(xValues.get(0)), TOL);

      // Check C1 continuity
      assertEquals(bci.firstDerivative(xValues.get(0) - TOL), bci.firstDerivative(xValues.get(0)), TOL * 1.e2);

      // Test sensitivity
      double[] yValues1Up = yValues[k].toArray();
      double[] yValues1Dw = yValues[k].toArray();
      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues[k].get(j) * (1. + EPS);
        yValues1Dw[j] = yValues[k].get(j) * (1. - EPS);
        BoundCurveInterpolator bciUp = CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Up), extrap, CurveExtrapolators.LOG_LINEAR);
        BoundCurveInterpolator bciDw = CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Dw), extrap, CurveExtrapolators.LOG_LINEAR);
        for (int i = 0; i < nKeys; ++i) {
          double res1 = 0.5 * (bciUp.interpolate(xKeys[i]) - bciDw.interpolate(xKeys[i])) / EPS / yValues[k].get(j);
          assertEquals(res1, bci.parameterSensitivity(xKeys[i]).get(j),
              Math.max(Math.abs(yValues[k].get(j)) * EPS, EPS) * 1.e2);//because gradient is NOT exact
        }
        yValues1Up[j] = yValues[k].get(j);
        yValues1Dw[j] = yValues[k].get(j);
      }
    }
  }

  public void differentIntervalsTest() {
    DoubleArray xValues = DoubleArray.of(
        1.0328724558967068, 1.2692381049172323, 2.8611430465380905, 4.296118458251132, 7.011992052151352,
        7.293354144919639, 8.557971037612713, 8.77306861567384, 10.572470371584489, 12.96945799507056);
    DoubleArray[] yValues = new DoubleArray[] {
        DoubleArray.of(1.1593075755231343, 2.794957672828094, 4.674733634811079, 5.517689918508841, 6.138447304104604,
            6.264375977142906, 6.581666492568779, 8.378685055774037, 10.005246918325483, 10.468304334744241),
        DoubleArray.of(9.95780079114617, 8.733013195721913, 8.192165283188197, 6.539369493529048, 6.3868683960757515,
            4.700471352238411, 4.555354921077598, 3.780781869340659, 2.299369456202763, 0.9182441378327986)
    };
    int nData = xValues.size();
    int nKeys = 100 * nData;
    double[] xKeys = new double[nKeys];
    double xMin = 0.;
    double xMax = xValues.get(nData - 1) + 2.;
    double step = (xMax - xMin) / nKeys;
    for (int i = 0; i < nKeys; ++i) {
      xKeys[i] = xMin + step * i;
    }

    CurveExtrapolator extrap = QuadraticLeftCurveExtrapolator.INSTANCE;

    int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      BoundCurveInterpolator bci = CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
          xValues, yValues[k], extrap, CurveExtrapolators.LOG_LINEAR);

      // Check C0 continuity
      assertEquals(bci.interpolate(xValues.get(0) - 1.e-14), bci.interpolate(xValues.get(0)), TOL);

      // Check C1 continuity
      assertEquals(bci.firstDerivative(xValues.get(0) - TOL), bci.firstDerivative(xValues.get(0)), TOL * 1.e2);

      // Test sensitivity
      double[] yValues1Up = yValues[k].toArray();
      double[] yValues1Dw = yValues[k].toArray();
      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues[k].get(j) * (1. + EPS);
        yValues1Dw[j] = yValues[k].get(j) * (1. - EPS);
        BoundCurveInterpolator bciUp = CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Up), extrap, CurveExtrapolators.LOG_LINEAR);
        BoundCurveInterpolator bciDw = CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
            xValues, DoubleArray.ofUnsafe(yValues1Dw), extrap, CurveExtrapolators.LOG_LINEAR);
        for (int i = 0; i < nKeys; ++i) {
          double res1 =
              0.5 * (bciUp.interpolate(xKeys[i]) - bciDw.interpolate(xKeys[i])) / EPS / yValues[k].get(j);
          assertEquals(res1, bci.parameterSensitivity(xKeys[i]).get(j),
              Math.max(Math.abs(yValues[k].get(j)) * EPS, EPS) * 1.e2);//because gradient is NOT exact
        }
        yValues1Up[j] = yValues[k].get(j);
        yValues1Dw[j] = yValues[k].get(j);
      }
    }
  }

  public void test_noRight() {
    BoundCurveInterpolator bci =
        CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, QL_EXTRAPOLATOR, QL_EXTRAPOLATOR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bci.interpolate(10d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bci.firstDerivative(10d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bci.parameterSensitivity(10d));
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(QL_EXTRAPOLATOR);
  }

}
