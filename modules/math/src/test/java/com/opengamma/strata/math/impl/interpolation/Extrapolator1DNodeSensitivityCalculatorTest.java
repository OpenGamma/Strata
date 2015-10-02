/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DCubicSplineDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class Extrapolator1DNodeSensitivityCalculatorTest {

  private static final FlatExtrapolator1D FLAT_INTERPOLATOR = new FlatExtrapolator1D();
  private static final LinearInterpolator1D LINEAR_INTERPOLATOR = new LinearInterpolator1D();
  private static final LinearExtrapolator1D LINEAR_EXTRAPOLATOR = new LinearExtrapolator1D(1e-6);
  private static final Interpolator1DCubicSplineDataBundle DATA;

  private static final Function1D<Double, Double> FUNCTION = new Function1D<Double, Double>() {
    private static final double A = -0.045;
    private static final double B = 0.03;
    private static final double C = 0.3;
    private static final double D = 0.05;

    @Override
    public Double evaluate(final Double x) {
      return (A + B * x) * Math.exp(-C * x) + D;
    }
  };

  static {
    final double[] t = new double[] {0.0, 0.5, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 15.0, 17.5, 20.0, 25.0, 30.0 };
    final int n = t.length;
    final double[] r = new double[n];
    for (int i = 0; i < n; i++) {
      r[i] = FUNCTION.evaluate(t[i]);
    }
    DATA = new Interpolator1DCubicSplineDataBundle(new ArrayInterpolator1DDataBundle(t, r));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData1() {
    LINEAR_EXTRAPOLATOR.getNodeSensitivitiesForValue(null, 102d, LINEAR_INTERPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData2() {
    FLAT_INTERPOLATOR.getNodeSensitivitiesForValue(null, 105d, LINEAR_INTERPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWithinRange1() {
    LINEAR_EXTRAPOLATOR.getNodeSensitivitiesForValue(DATA, 20d, LINEAR_INTERPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWithinRange2() {
    FLAT_INTERPOLATOR.getNodeSensitivitiesForValue(DATA, 20d, LINEAR_INTERPOLATOR);
  }

  @Test
  public void firstDerivativeTest() {
    double a = 1.0;
    double b = 1.5;
    double c = -0.5;
    double[] x = new double[] {0., 2., 5. };
    int n = x.length;
    double[] y = new double[n];
    for (int i = 0; i < n; i++) {
      y[i] = a + b * x[i] + c * x[i] * x[i];
    }
    Interpolator1D interpolator = new NaturalCubicSplineInterpolator1D();
    Interpolator1DDataBundle db = interpolator.getDataBundle(x, y);
    Double grad = interpolator.firstDerivative(db, x[n - 1]);
    Function1D<Double, Double> func = interpolator.getFunction(db);
    ScalarFirstOrderDifferentiator diff = new ScalarFirstOrderDifferentiator();
    Function1D<Double, Boolean> domain = new Function1D<Double, Boolean>() {
      @Override
      public Boolean evaluate(Double x) {
        return x <= 5.0;
      }
    };
    Function1D<Double, Double> gradFunc = diff.differentiate(func, domain);

    assertEquals(gradFunc.evaluate(x[n - 1]), grad, 1e-8);
  }
}
