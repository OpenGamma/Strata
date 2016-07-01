/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link InterpolatorCurveExtrapolator}.
 */
@Test
public class InterpolatorCurveExtrapolatorTest {

  private static final CurveExtrapolator INT_EXTRAPOLATOR = InterpolatorCurveExtrapolator.INSTANCE;

  private static final double TOL = 1.e-14;

  public void test_basics() {
    assertEquals(INT_EXTRAPOLATOR.getName(), InterpolatorCurveExtrapolator.NAME);
    assertEquals(INT_EXTRAPOLATOR.toString(), InterpolatorCurveExtrapolator.NAME);
  }

  public void sameIntervalsTest() {
    DoubleArray xValues = DoubleArray.of(-1., 0., 1., 2., 3., 4., 5., 6., 7., 8.);
    DoubleArray[] yValues = new DoubleArray[] {
        DoubleArray.of(1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001),
        DoubleArray.of(11., 11., 8., 5., 1.001, 1.001, 5., 8., 11., 11.),
        DoubleArray.of(1.001, 1.001, 5., 8., 9., 9., 11., 12., 18., 18.)
    };
    int nKeys = 100;
    double[] keys = new double[nKeys];
    double interval = 0.061;
    for (int i = 0; i < nKeys; ++i) {
      keys[i] = xValues.get(0) + interval * i;
    }

    CurveExtrapolator extrap = InterpolatorCurveExtrapolator.INSTANCE;
    int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      BoundCurveInterpolator boundInterp = CurveInterpolators.SQUARE_LINEAR.bind(xValues, yValues[k], extrap, extrap);
      AbstractBoundCurveInterpolator baseInterp = (AbstractBoundCurveInterpolator) boundInterp;
      for (int j = 0; j < nKeys; ++j) {
        // value
        assertEquals(boundInterp.interpolate(keys[j]), baseInterp.doInterpolate(keys[j]), TOL);
        // derivative 
        assertEquals(boundInterp.firstDerivative(keys[j]), baseInterp.doFirstDerivative(keys[j]), TOL);
        // sensitivity
        assertTrue(boundInterp.parameterSensitivity(keys[j]).equalWithTolerance(baseInterp.doParameterSensitivity(keys[j]), TOL));
      }
    }
  }

  public void differentIntervalsTest() {
    DoubleArray xValues = DoubleArray.of(
        1.0328724558967068, 1.2692381049172323, 2.8611430465380905, 4.296118458251132, 7.011992052151352,
        7.293354144919639, 8.557971037612713, 8.77306861567384, 10.572470371584489, 12.96945799507056);
    DoubleArray[] yValues = new DoubleArray[] {
        DoubleArray.of(
            1.1593075755231343, 2.794957672828094, 4.674733634811079, 5.517689918508841, 6.138447304104604,
            6.264375977142906, 6.581666492568779, 8.378685055774037, 10.005246918325483, 10.468304334744241),
        DoubleArray.of(
            9.95780079114617, 8.733013195721913, 8.192165283188197, 6.539369493529048, 6.3868683960757515,
            4.700471352238411, 4.555354921077598, 3.780781869340659, 2.299369456202763, 0.9182441378327986)
    };
    int nKeys = 100;
    double[] keys = new double[nKeys];
    double interval = 0.061;
    for (int i = 0; i < nKeys; ++i) {
      keys[i] = xValues.get(0) + interval * i;
    }

    CurveExtrapolator extrap = InterpolatorCurveExtrapolator.INSTANCE;
    int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      BoundCurveInterpolator boundInterp = CurveInterpolators.SQUARE_LINEAR.bind(xValues, yValues[k], extrap, extrap);
      AbstractBoundCurveInterpolator baseInterp = (AbstractBoundCurveInterpolator) boundInterp;
      for (int j = 0; j < nKeys; ++j) {
        // value
        assertEquals(boundInterp.interpolate(keys[j]), baseInterp.doInterpolate(keys[j]), TOL);
        // derivative 
        assertEquals(boundInterp.firstDerivative(keys[j]), baseInterp.doFirstDerivative(keys[j]), TOL);
        // sensitivity
        assertTrue(boundInterp.parameterSensitivity(keys[j]).equalWithTolerance(baseInterp.doParameterSensitivity(keys[j]), TOL));
      }
    }
  }

  public void test_serialization() {
    assertSerialization(INT_EXTRAPOLATOR);
  }

}
