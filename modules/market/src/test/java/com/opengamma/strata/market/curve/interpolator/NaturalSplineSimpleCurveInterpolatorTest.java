/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link NaturalSplineSimpleCurveInterpolator}.
 */
@Test
public class NaturalSplineSimpleCurveInterpolatorTest {

  private static final CurveInterpolator INTERP = CurveInterpolators.NATURAL_SPLINE_SIMPLE;
  private static final CurveInterpolator BASE_INTERP = CurveInterpolators.NATURAL_SPLINE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;
  private static final double TOL = 1.0e-12;

  public void basicTest() {
    assertEquals(INTERP.getName(), NaturalSplineSimpleCurveInterpolator.NAME);
    assertEquals(INTERP.toString(), NaturalSplineSimpleCurveInterpolator.NAME);
  }

  public void positiveDataTest() {
    DoubleArray xValues = DoubleArray.of(0.5, 1.0, 2.5, 4.2, 10.0, 15.0, 30.0);
    DoubleArray yValues = DoubleArray.of(4.0, 2.0, 1.0, 5.0, 10.0, 3.5, -2.0);
    int nData = yValues.size();
    DoubleArray keys = DoubleArray.of(xValues.get(0), 0.7, 1.2, 7.8, 10.0, 17.52, 25.0, xValues.get(nData - 1));
    int nKeys = keys.size();
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, yValues);
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)), TOL);
      // first derivative
      assertEquals(bound.firstDerivative(keys.get(i)), boundBase.firstDerivative(keys.get(i)), TOL);
    }
  }

  public void negativeDataTest() {
    DoubleArray xValues = DoubleArray.of(-34.5, -27.0, -12.5, 4.2, 10.0, 15.0, 20.3);
    DoubleArray yValues = DoubleArray.of(4.0, 2.0, 1.0, 5.0, 10.0, 3.5, -2.0);
    int nData = yValues.size();
    DoubleArray keys = DoubleArray.of(xValues.get(0), -27.7, -2.2, 1.8, 10.0, 15.2, 19.35, xValues.get(nData - 1));
    int nKeys = keys.size();
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, yValues);
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)), TOL);
      // first derivative
      assertEquals(bound.firstDerivative(keys.get(i)), boundBase.firstDerivative(keys.get(i)), TOL);
    }
  }

  public void extrapolationTest() {
    DoubleArray xValues = DoubleArray.of(0.5, 3.0, 5.0, 10.0, 20.0);
    DoubleArray yValues = DoubleArray.of(0.03, 0.05, 0.02, 0.01, 0.01);
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, yValues, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    DoubleArray keys = DoubleArray.of(0.01, 0.5, 2.65, 12.5, 20.0, 24.5);
    int nKeys = keys.size();
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)), TOL);
      // first derivative
      assertEquals(bound.firstDerivative(keys.get(i)), boundBase.firstDerivative(keys.get(i)), TOL);
    }

  }

  //-------------------------------------------------------------------------
  public void sensitivityTest() {
    DoubleArray xValues = DoubleArray.of(0.5, 1.0, 2.5, 4.2, 10.0, 15.0, 30.0);
    DoubleArray yValues = DoubleArray.of(4.0, 2.0, 1.0, 5.0, 10.0, 3.5, -2.0);
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    assertThrowsIllegalArg(() -> bound.parameterSensitivity(4.5));
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(INTERP);
  }

}
