/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;

/**
 * Test {@link ProductNaturalSplineCurveInterpolator}.
 */
@Test
public class ProductNaturalSplineCurveInterpolatorTest {

  private static final CurveInterpolator INTERP = ProductNaturalSplineCurveInterpolator.INSTANCE;
  private static final CurveInterpolator BASE_INTERP = NaturalSplineCurveInterpolator.INSTANCE;
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;
  private static final ScalarFirstOrderDifferentiator DIFF_CALC =
      new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFieldFirstOrderDifferentiator SENS_CALC =
      new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);

  public void sampleDataTest() {
    DoubleArray xValues = DoubleArray.of(0.5, 1.0, 2.5, 4.2, 10.0, 15.0, 30.0);
    DoubleArray yValues = DoubleArray.of(4.0, 2.0, 1.0, 5.0, 10.0, 3.5, -2.0);
    int nData = yValues.size();
    DoubleArray pValues = DoubleArray.of(nData, i -> xValues.get(i) * yValues.get(i));
    Function<Double, Boolean> domain = new Function<Double, Boolean>() {
      @Override
      public Boolean apply(Double x) {
        return x >= xValues.get(0) && x <= xValues.get(nData - 1);
      }
    };
    DoubleArray keys = DoubleArray.of(xValues.get(0), 0.7, 1.2, 7.8, 10.0, 17.52, 25.0, xValues.get(nData - 1));
    int nKeys = keys.size();
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, pValues);
    Function<Double, Double> funcDeriv = x -> bound.interpolate(x);
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)) / keys.get(i), TOL);
      // first derivative
      double firstExp = DIFF_CALC.differentiate(funcDeriv, domain).apply(keys.get(i));
      assertEquals(bound.firstDerivative(keys.get(i)), firstExp, EPS);
      // parameter sensitivity
      int index = i;
      Function<DoubleArray, Double> funcSensi = x -> INTERP.bind(xValues, x).interpolate(keys.get(index));
      DoubleArray sensExp = SENS_CALC.differentiate(funcSensi).apply(yValues);
      assertTrue(DoubleArrayMath.fuzzyEquals(bound.parameterSensitivity(keys.get(i)).toArray(), sensExp.toArray(), EPS));
    }
  }

  public void negativeDataTest() {
    DoubleArray xValues = DoubleArray.of(-34.5, -27.0, -22.5, -14.2, -10.0, -5.0, -0.3);
    DoubleArray yValues = DoubleArray.of(4.0, 2.0, 1.0, 5.0, 10.0, 3.5, -2.0);
    int nData = yValues.size();
    DoubleArray pValues = DoubleArray.of(nData, i -> xValues.get(i) * yValues.get(i));
    Function<Double, Boolean> domain = new Function<Double, Boolean>() {
      @Override
      public Boolean apply(Double x) {
        return x >= xValues.get(0) && x <= xValues.get(nData - 1);
      }
    };
    DoubleArray keys = DoubleArray.of(xValues.get(0), -27.7, -21.2, -17.8, -10.0, -1.52, -0.35, xValues.get(nData - 1));
    int nKeys = keys.size();
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, pValues);
    Function<Double, Double> funcDeriv = x -> bound.interpolate(x);
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)) / keys.get(i), TOL);
      // first derivative
      double firstExp = DIFF_CALC.differentiate(funcDeriv, domain).apply(keys.get(i));
      assertEquals(bound.firstDerivative(keys.get(i)), firstExp, EPS);
      // parameter sensitivity
      int index = i;
      Function<DoubleArray, Double> funcSensi = x -> INTERP.bind(xValues, x).interpolate(keys.get(index));
      DoubleArray sensExp = SENS_CALC.differentiate(funcSensi).apply(yValues);
      assertTrue(DoubleArrayMath.fuzzyEquals(bound.parameterSensitivity(keys.get(i)).toArray(), sensExp.toArray(), EPS));
    }
  }

  public void linearDataTest() {
    DoubleArray xValues = DoubleArray.of(0.5, 2.0, 3.0, 4.0, 5.0);
    DoubleArray yValues = DoubleArray.of(1.0, 4.0, 6.0, 8.0, 10.0);
    int nData = yValues.size();
    DoubleArray pValues = DoubleArray.of(nData, i -> xValues.get(i) * yValues.get(i));
    Function<Double, Boolean> domain = new Function<Double, Boolean>() {
      @Override
      public Boolean apply(Double x) {
        return x >= xValues.get(0) && x <= xValues.get(nData - 1);
      }
    };
    DoubleArray keys = DoubleArray.of(xValues.get(0), 1.1, 2.0, 4.7, xValues.get(nData - 1));
    int nKeys = keys.size();
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    BoundCurveInterpolator boundBase = BASE_INTERP.bind(xValues, pValues);
    Function<Double, Double> funcDeriv = x -> bound.interpolate(x);
    for (int i = 0; i < nKeys; ++i) {
      // interpolate
      assertEquals(bound.interpolate(keys.get(i)), boundBase.interpolate(keys.get(i)) / keys.get(i), TOL);
      // first derivative
      double firstExp = DIFF_CALC.differentiate(funcDeriv, domain).apply(keys.get(i));
      assertEquals(bound.firstDerivative(keys.get(i)), firstExp, EPS);
      // parameter sensitivity
      int index = i;
      Function<DoubleArray, Double> funcSensi = x -> INTERP.bind(xValues, x).interpolate(keys.get(index));
      DoubleArray sensExp = SENS_CALC.differentiate(funcSensi).apply(yValues);
      assertTrue(DoubleArrayMath.fuzzyEquals(bound.parameterSensitivity(keys.get(i)).toArray(), sensExp.toArray(), EPS));
    }
  }

  public void smallKeyTest() {
    DoubleArray xValues = DoubleArray.of(1e-13, 3e-8, 2e-5);
    DoubleArray yValues = DoubleArray.of(1.0, 13.2, 1.5);
    double keyDw = 1.0e-12;
    BoundCurveInterpolator bound = INTERP.bind(xValues, yValues);
    assertThrowsIllegalArg(() -> bound.interpolate(keyDw));
    assertThrowsIllegalArg(() -> bound.firstDerivative(keyDw));
    assertThrowsIllegalArg(() -> bound.parameterSensitivity(keyDw));
  }

  public void getterTest() {
    assertEquals(INTERP.getName(), ProductNaturalSplineCurveInterpolator.NAME);
    assertEquals(INTERP.toString(), ProductNaturalSplineCurveInterpolator.NAME);
  }

  public void test_serialization() {
    assertSerialization(INTERP);
  }

}
