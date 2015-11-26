/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Tests related to the exponential interpolator.
 */
@Test
public class ExponentialInterpolator1DTest {
  private static final Interpolator1D INTERPOLATOR = new ExponentialInterpolator1D();
  private static final double EPS = 1e-4;
  private static final double REL_TOL = 1.0e-10;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDataBundle() {
    INTERPOLATOR.interpolate(null, 2.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowValue() {
    INTERPOLATOR.interpolate(INTERPOLATOR.getDataBundleFromSortedArrays(new double[] {1, 2 }, new double[] {1, 2 }), -4.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighValue() {
    INTERPOLATOR.interpolate(INTERPOLATOR.getDataBundleFromSortedArrays(new double[] {1, 2 }, new double[] {1, 2 }), -4.);
  }

  @Test
  public void testDataBundleType1() {
    assertEquals(INTERPOLATOR.getDataBundle(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), ArrayInterpolator1DDataBundle.class);
  }

  @Test
  public void testDataBundleType2() {
    assertEquals(INTERPOLATOR.getDataBundleFromSortedArrays(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), ArrayInterpolator1DDataBundle.class);
  }

  /**
   * Recover a single exponential function
   */
  @Test
  public void exponentialFunctionTest() {
    /* positive */
    double a1 = 3.5;
    double b1 = 1.4;
    Function<Double, Double> func1 = createExpFunction(a1, b1);
    double[] xData1 = new double[] {-2.2, -3.0 / 11.0, 0.0, 0.01, 3.0, 9.5 };
    double[] keys1 = new double[] {-2.05, -2.1, -1.8, -1.0 / 11.0, 0.05, 0.5, 4.5, 8.25, 9.2 };
    int dataSize1 = xData1.length;
    double[] yData1 = new double[dataSize1];
    for (int i = 0; i < dataSize1; ++i) {
      yData1[i] = func1.apply(xData1[i]);
    }
    int keySize1 = keys1.length;
    double[] expectedValues1 = new double[keySize1];
    for (int i = 0; i < keySize1; ++i) {
      expectedValues1[i] = func1.apply(keys1[i]);
    }
    testInterpolation(xData1, yData1, keys1, expectedValues1, false);
    /* negative */
    double a2 = -1.82;
    double b2 = 0.2;
    Function<Double, Double> func2 = createExpFunction(a2, b2);
    double[] xData2 = new double[] {-2.2, -3.0 / 11.0, 0.0, 0.01, 3.0, 12.5 };
    double[] keys2 = new double[] {-2.1, -1.8, -1.0 / 11.0, 0.05, 0.5, 4.5, 8.25 };
    int dataSize2 = xData2.length;
    double[] yData2 = new double[dataSize2];
    for (int i = 0; i < dataSize2; ++i) {
      yData2[i] = func2.apply(xData2[i]);
    }
    int keySize2 = keys2.length;
    double[] expectedValues2 = new double[keySize2];
    for (int i = 0; i < keySize2; ++i) {
      expectedValues2[i] = func2.apply(keys2[i]);
    }
    testInterpolation(xData2, yData2, keys2, expectedValues2, false);
  }

  /**
   * Recover piecewise exponential function
   */
  @SuppressWarnings("unchecked")
  @Test
  public void piecewiseExponentialFunctionTest() {
    /* positive */
    double[] a1 = new double[] {2.5, 2.2, 2.7, 5.6, 0.7 };
    double[] xData1 = new double[] {-2.2, -3.0 / 11.0, 0.2, 1.1, 3.0, 9.5};
    int nIntervals = a1.length;
    double[] b1 = new double[nIntervals];
    double[] yData1 = new double[nIntervals + 1];
    // introducing b1 and yData1 such that the piecewise function becomes continuous
    Function<Double, Double>[] func1 = new Function[nIntervals];
    b1[0] = 1.4;
    func1[0] = createExpFunction(a1[0], b1[0]);
    yData1[0] = func1[0].apply(xData1[0]);
    yData1[1] = func1[0].apply(xData1[1]);
    for (int i = 1; i < nIntervals; ++i) {
      b1[i] = b1[i - 1] - Math.log(a1[i] / a1[i - 1]) / xData1[i];
      func1[i] = createExpFunction(a1[i], b1[i]);
      yData1[i + 1] = func1[i].apply(xData1[i + 1]);
    }
    double[] keys1 = new double[] {-2.05, -2.1, -1.8, -1.0 / 11.0, 0.0, 0.05,
      0.5, 1.2, 3.3, 4.5, 5.2, 7.33, 8.25, 9.2 };
    int keySize1 = keys1.length;
    double[] expectedValues1 = new double[keySize1];
    for (int i = 0; i < keySize1; ++i) {
      int index = FunctionUtils.getLowerBoundIndex(DoubleArray.copyOf(xData1), keys1[i]);
      expectedValues1[i] = func1[index].apply(keys1[i]);
    }
    testInterpolation(xData1, yData1, keys1, expectedValues1, false);
    /* negative */
    double[] a2 = new double[] {-2.5, -2.1, -2.2, -5.6, -1.7 };
    double[] xData2 = new double[] {-2.2, -3.0 / 22.0, 0.2, 1.2, 3.0, 9.5};
    nIntervals = a2.length;
    double[] b2 = new double[nIntervals];
    double[] yData2 = new double[nIntervals + 1];
    // introducing b2 and yData2 such that the piecewise function becomes continuous
    Function<Double, Double>[] func2 = new Function[nIntervals];
    b2[0] = 1.4;
    func2[0] = createExpFunction(a2[0], b2[0]);
    yData2[0] = func2[0].apply(xData2[0]);
    yData2[1] = func2[0].apply(xData2[1]);
    for (int i = 1; i < nIntervals; ++i) {
      b2[i] = b2[i - 1] - Math.log(a2[i] / a2[i - 1]) / xData2[i];
      func2[i] = createExpFunction(a2[i], b2[i]);
      yData2[i + 1] = func2[i].apply(xData2[i + 1]);
    }
    double[] keys2 = new double[] {-2.05, -2.2, -1.8, -1.0 / 22.0, 0.0, 0.05,
      0.5, 2.2, 3.3, 4.5, 5.2, 7.33, 8.25, 9.2 };
    int keySize2 = keys2.length;
    double[] expectedValues2 = new double[keySize2];
    for (int i = 0; i < keySize2; ++i) {
      int index = FunctionUtils.getLowerBoundIndex(DoubleArray.copyOf(xData2), keys2[i]);
      expectedValues2[i] = func2[index].apply(keys2[i]);
    }
    testInterpolation(xData2, yData2, keys2, expectedValues2, false);
  }

  /**
   * Recover flat curve
   */
  @Test
  public void strightLineTest() {
    /* positive */
    double a1 = 2.5;
    double b1 = 0.0;
    Function<Double, Double> func1 = createExpFunction(a1, b1);
    double[] xData1 = new double[] {-2.2, -1.1, -0.5, -3.0 / 11.0, 0.0, 0.01, 1.05, 2.6, 3.4, 5.1 };
    double[] keys1 = new double[] {-2.1, -1.8, -1.0 / 11.0, 0.05, 0.5, 4.5 };
    int dataSize1 = xData1.length;
    double[] yData1 = new double[dataSize1];
    for (int i = 0; i < dataSize1; ++i) {
      yData1[i] = func1.apply(xData1[i]);
    }
    int keySize1 = keys1.length;
    double[] expectedValues1 = new double[keySize1];
    for (int i = 0; i < keySize1; ++i) {
      expectedValues1[i] = func1.apply(keys1[i]);
    }
    testInterpolation(xData1, yData1, keys1, expectedValues1, false);
    /* negative */
    double a2 = -3.82;
    double b2 = 0.0;
    Function<Double, Double> func2 = createExpFunction(a2, b2);
    double[] xData2 = new double[] {-12.0, 0.15, 1.1, 3.0, 9.2, 12.5 };
    double[] keys2 = new double[] {-11.0, -5.41, 0.5, 2.22, 4.5, 5.78, 7.4, 10.1, 11.25 };
    int dataSize2 = xData2.length;
    double[] yData2 = new double[dataSize2];
    for (int i = 0; i < dataSize2; ++i) {
      yData2[i] = func2.apply(xData2[i]);
    }
    int keySize2 = keys2.length;
    double[] expectedValues2 = new double[keySize2];
    for (int i = 0; i < keySize2; ++i) {
      expectedValues2[i] = func2.apply(keys2[i]);
    }
    testInterpolation(xData2, yData2, keys2, expectedValues2, false);
  }

  /**
   * Node point is treated as a point in the right interval, except the rightmost node point.
   */
  @Test
  public void nodePointsTest() {
    /* positive */
    double[] xData1 = new double[] {-7.2, -4.4, -2.1, -0.1, 5.0, 5.87 };
    double[] yData1 = new double[] {3.0, 28.0, 31.2, 13.2, 19.3, 20.9 };
    int dataSize1 = xData1.length;
    double[] keys1 = Arrays.copyOf(xData1, dataSize1 - 1);
    double[] expectedValues1 = Arrays.copyOf(yData1, dataSize1 - 1);
    testInterpolation(xData1, yData1, keys1, expectedValues1, true);
    testInterpolation(xData1, yData1, new double[] {xData1[dataSize1 - 1] }, new double[] {yData1[dataSize1 - 1] },
        false);
    /* negative */
    double[] xData2 = new double[] {-12.2, -3.4, -1.2, 0.26, 11.0, 25.22 };
    double[] yData2 = new double[] {-2.0, -13.0, -2.2, -3.5, -9.7, -16.6 };
    int dataSize2 = xData2.length;
    double[] keys2 = Arrays.copyOf(xData2, dataSize2 - 1);
    double[] expectedValues2 = Arrays.copyOf(yData2, dataSize2 - 1);
    testInterpolation(xData2, yData2, keys2, expectedValues2, true);
    testInterpolation(xData2, yData2, new double[] {xData2[dataSize2 - 1] }, new double[] {yData2[dataSize2 - 1] },
        false);
  }

  /**
   * sign is not the same
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void illegalYDataTest1() {
    double[] xData = new double[] {-6.2, -3.4, -2.1, 0.15, 5.0, 5.87 };
    double[] yData = new double[] {3.0, 2.0, 1.2, -2.2, 1.3, 2.9 };
    Interpolator1DDataBundle data = INTERPOLATOR.getDataBundle(xData, yData);
    INTERPOLATOR.interpolate(data, 1.0);
  }

  /**
   * sign is not the same
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void illegalYDataTest2() {
    double[] xData = new double[] {-6.2, -3.4, -2.1, 0.15, 5.0, 5.87 };
    double[] yData = new double[] {-3.0, -2.0, 1.2, -2.2, -1.3, -2.9 };
    Interpolator1DDataBundle data = INTERPOLATOR.getDataBundleFromSortedArrays(xData, yData);
    INTERPOLATOR.interpolate(data, 1.0);
  }

  /**
   * test instances via factory
   */
  @Test
  public void factoryTest() {
    Interpolator1D interp1 = new ExponentialInterpolator1D();
    Interpolator1D interp2 = Interpolator1DFactory.EXPONENTIAL_INSTANCE;
    Interpolator1D interp3 = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.EXPONENTIAL);
    Interpolator1D interp4 = Interpolator1DFactory.getInterpolator("Exponential");
    assertEquals(interp1, interp2);
    assertEquals(interp1, interp3);
    assertEquals(interp1, interp4);
  }

  private void testInterpolation(double[] xData, double[] yData, double[] keys, double[] expectedValues, boolean isNode) {
    Interpolator1DDataBundle data = INTERPOLATOR.getDataBundleFromSortedArrays(xData, yData);
    int dataSize = xData.length;
    int keySize = keys.length;
    for (int i = 0; i < keySize; ++i) {
      /* Test interpolant value at key */
      double result = INTERPOLATOR.interpolate(data, keys[i]);
      assertEqualsRelative(expectedValues[i], result, REL_TOL);
      /* Test gradient at key */
      double expectedGrad = 0.0;
      if (keys[i] + EPS > xData[dataSize - 1]) {
        double downDown = INTERPOLATOR.interpolate(data, keys[i] - 2.0 * EPS);
        double down = INTERPOLATOR.interpolate(data, keys[i] - EPS);
        expectedGrad = 0.5 * (3.0 * result + downDown - 4.0 * down) / EPS;
      } else if (keys[i] - EPS < xData[0] || isNode) {
        double up = INTERPOLATOR.interpolate(data, keys[i] + EPS);
        double upUp = INTERPOLATOR.interpolate(data, keys[i] + 2.0 * EPS);
        expectedGrad = 0.5 * (4.0 * up - 3.0 * result - upUp) / EPS;
      } else {
        double up = INTERPOLATOR.interpolate(data, keys[i] + EPS);
        double down = INTERPOLATOR.interpolate(data, keys[i] - EPS);
        expectedGrad = 0.5 * (up - down) / EPS;
      }
      assertEqualsRelative(expectedGrad, INTERPOLATOR.firstDerivative(data, keys[i]), EPS);
      /* Test node sensitivities at key */
      double[] sense = INTERPOLATOR.getNodeSensitivitiesForValue(data, keys[i]);
      for (int j = 0; j < dataSize; ++j) {
        double[] yDataUp = Arrays.copyOf(yData, dataSize);
        double[] yDataDw = Arrays.copyOf(yData, dataSize);
        yDataUp[j] += EPS;
        yDataDw[j] -= EPS;
        Interpolator1DDataBundle dataUp = INTERPOLATOR.getDataBundleFromSortedArrays(xData, yDataUp);
        Interpolator1DDataBundle dataDw = INTERPOLATOR.getDataBundleFromSortedArrays(xData, yDataDw);
        double valueUp = INTERPOLATOR.interpolate(dataUp, keys[i]);
        double valueDw = INTERPOLATOR.interpolate(dataDw, keys[i]);
        double expectedSense = 0.5 * (valueUp - valueDw) / EPS;
        assertEquals(expectedSense, sense[j], EPS);
      }
    }
  }

  private void assertEqualsRelative(double expected, double obtained, double relativeTol) {
    assertEquals(expected, obtained, Math.max(Math.abs(expected), 1.0) * relativeTol);
  }

  private Function<Double, Double> createExpFunction(final double a, final double b) {
    return new Function<Double, Double>() {
      @Override
      public Double apply(Double value) {
        return a * Math.exp(b * value);
      }
    };
  }
}
