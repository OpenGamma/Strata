/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Here we use the following notation
 * h_i = xValues[i+1] - xValues[i]
 * delta_i = (yValues[i+1] - yValues[i])/h_i
 * d_i = dF(x)/dx |x=xValues[i] where F(x) is the piecewise interpolation function
 */
public class PiecewiseCubicHermiteSplineInterpolatorTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  /**
   * Test for the case with boundary value d_0 = 0
   */
  @Test
  public void bvCase1Test() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[] yValues = new double[] {1., 2., 10., 11.};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {
        {-2. / 9., 11. / 9., 0., 1.}, {-112. / 9., 56. / 3., 16. / 9., 2.}, {-2. / 9., -80. / 144., 16. / 9., 10.}};

    PiecewisePolynomialInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * Test for the case with boundary value d_0 = 3 * delta_0
   */
  @Test
  public void bvCase2Test() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[] yValues = new double[] {9., 10., 1., 3.};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {{1., -3., 3., 9.}, {18., -27., 0., 10.}, {2., 0., 0., 1.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * Test for the case with boundary value d_0 = ((2 * h_0 + h_1) * delta_0 - h_0 * delta_1)/(h_0 + h_1)
   */
  @Test
  public void bvCase3Test() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[] yValues = new double[] {2., 3., 2., 3.};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {{0., -1., 2., 2.}, {2., -3., 0., 3.}, {0., 1., 0., 2.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * Test for the case with boundary value d_0 = ((2 * h_0 + h_1) * delta_0 - h_0 * delta_1)/(h_0 + h_1) corresponding to the other branch
   */
  @Test
  public void bvCase3AnotherTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[] yValues = new double[] {2., 3., 2., 1.};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {{0., -1., 2., 2.}, {1., -2., 0., 3.}, {0., 0., -1., 2.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * d_i =0 if delta_i = 0 or delta_{i-1} = 0
   */
  @Test
  public void coincideYvaluesTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[] yValues = new double[] {1., 2., 2., 3.};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {{-1. / 2., 0., 1.5, 1.}, {0., 0., 0., 2.}, {-0.5, 1.5, 0., 2.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * Intervals have different length
   */
  @Test
  public void diffIntervalsTest() {
    final double[] xValues = new double[] {1., 2., 5., 8.};
    final double[] yValues = new double[] {2., 3., 2., 1.};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {
        {-2. / 3., 1. / 3., 4. / 3., 2.}, {1. / 27., -2. / 9., 0., 3.}, {0., 0., -1. / 3., 2.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * Linear interpolation for 2 data points
   */
  @Test
  public void linearTest() {
    final double[] xValues = new double[] {1., 2.};
    final double[] yValues = new double[] {1., 4.};

    final int nIntervalsExp = 1;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {{0., 0., 3., 1.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = result.getCoefMatrix().get(i, j) == 0. ? 1. : Math.abs(result.getCoefMatrix().get(i, j));
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  //(enabled=false)
  @Test
  public void monotonicTest() {
    PiecewiseCubicHermiteSplineInterpolator interpolator = new PiecewiseCubicHermiteSplineInterpolator();

    final double[] xValues = new double[] {0., 0.3, 0.6, 1.5, 2.7, 3.4, 4.8, 5.9};
    final double[] yValues = new double[] {1.0, 1.2, 1.5, 2.0, 2.1, 3.0, 3.1, 3.3};
    final int nPts = 300;
    double old = yValues[0] * xValues[0];
    for (int i = 0; i < nPts; ++i) {
      final double key = 0.0 + i * 5.9 / (nPts - 1);
      final double value = interpolator.interpolate(xValues, yValues, key);
      assertThat(value >= old).isTrue();
      old = value;
    }
  }

  /**
   * 
   */
  @Test
  public void nullXvaluesTest() {
    double[] xValues = null;
    double[] yValues = new double[] {1., 2., 3., 4.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullYvaluesTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = null;

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void wrongDatalengthTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[] yValues = new double[] {1., 2., 3., 4.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void shortDataLengthTest() {
    double[] xValues = new double[] {1.};
    double[] yValues = new double[] {4.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNxValuesTest() {
    double[] xValues = new double[] {1., 2., Double.NaN, 4.};
    double[] yValues = new double[] {1., 2., 3., 4.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNyValuesTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {1., 2., Double.NaN, 4.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infxValuesTest() {
    double[] xValues = new double[] {1., 2., 3., INF};
    double[] yValues = new double[] {1., 2., 3., 4.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infyValuesTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {1., 2., 3., INF};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void coincideXvaluesTest() {
    double[] xValues = new double[] {1., 2., 3., 3.};
    double[] yValues = new double[] {1., 2., 3., 4.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   *  Tests for multi-dimensions with all of the endpoint conditions
   */
  @Test
  public void allBvsMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[][] yValues =
        new double[][] {{1., 2., 10., 11.}, {9., 10., 1., 3.}, {2., 3., 2., 3.}, {1., 2., 2., 3.}, {2., 3., 2., 1.}};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 5;
    final double[][] coefsMatExp = new double[][] {
        {-2. / 9., 11. / 9., 0., 1.},
        {1., -3., 3., 9.},
        {0., -1., 2., 2.},
        {-1. / 2., 0., 1.5, 1.},
        {0., -1., 2., 2.},
        {-112. / 9., 56. / 3., 16. / 9., 2.},
        {18., -27., 0., 10.},
        {2., -3., 0., 3.},
        {0., 0., 0., 2.},
        {1., -2., 0., 3.},
        {-2. / 9., -80. / 144., 16. / 9., 10.},
        {2., 0., 0., 1.},
        {0., 1., 0., 2.},
        {-0.5, 1.5, 0., 2.},
        {0., 0., -1., 2.}};

    PiecewisePolynomialInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp * dimExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * Linear interpolation for 2 data points
   */
  @Test
  public void linearMultiTest() {
    final double[] xValues = new double[] {1., 2.};
    final double[][] yValues = new double[][] {{1., 4.}, {1., 1. / 3.}};

    final int nIntervalsExp = 1;
    final int orderExp = 4;
    final int dimExp = 2;
    final double[][] coefsMatExp = new double[][] {{0., 0., 3., 1.}, {0., 0., -2. / 3., 1.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp * dimExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * Intervals have different length
   */
  @Test
  public void diffIntervalsMultiTest() {
    final double[] xValues = new double[] {1., 2., 5., 8.};
    final double[][] yValues = new double[][] {{2., 3., 2., 1.}, {-1., 3., 6., 7.}};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 2;
    final double[][] coefsMatExp = new double[][] {
        {-2. / 3., 1. / 3., 4. / 3., 2.},
        {-53. / 36, 13. / 18., 19. / 4., -1.},
        {1. / 27., -2. / 9., 0., 3.},
        {5. / 162., -19. / 54., 16. / 9., 3.},
        {0., 0., -1. / 3., 2.},
        {-1. / 54., 0., 1. / 2., 6.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp * dimExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }
  }

  /**
   * 
   */
  @Test
  public void nullXvaluesMultiTest() {
    double[] xValues = null;
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {1., 5., 3., 4.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullYvaluesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = null;

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void wrongDatalengthMultiTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void shortDataLengthMultiTest() {
    double[] xValues = new double[] {1.};
    double[][] yValues = new double[][] {{4.}, {1.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNxValuesMultiTest() {
    double[] xValues = new double[] {1., 2., Double.NaN, 4.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNyValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {1., 2., Double.NaN, 4.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infxValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., INF};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infyValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {1., 2., 3., INF}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void coincideXvaluesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 3.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * Derive value of the underlying cubic spline function at the value of xKey
   */
  @Test
  public void interpolantsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[][] yValues = new double[][] {{2., 3., 2., 1.}, {1., 2., 10., 11.}};
    final double[][] xKey = new double[][] {{-1., 0.5, 1.5}, {2.5, 3.5, 4.5}};

    final double[][][] resultValuesExpected =
        new double[][][] {
            {{-6., 21. / 8.}, {23. / 3., 6.}}, {{3. / 4., 3. / 2.}, {4. / 3., 193. / 18.}},
            {{11. / 4., 1. / 2.}, {23. / 18., 32. / 3.}}};

    final int yDim = yValues.length;
    final int keyLength = xKey[0].length;
    final int keyDim = xKey.length;

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    double value = interp.interpolate(xValues, yValues[0], xKey[0][0]);
    {
      final double ref = resultValuesExpected[0][0][0] == 0. ? 1. : Math.abs(resultValuesExpected[0][0][0]);
      assertThat(value).isCloseTo(resultValuesExpected[0][0][0], offset(ref * EPS));
    }

    DoubleArray valuesVec1 = interp.interpolate(xValues, yValues, xKey[0][0]);
    for (int i = 0; i < yDim; ++i) {
      final double ref = resultValuesExpected[0][i][0] == 0. ? 1. : Math.abs(resultValuesExpected[0][i][0]);
      assertThat(valuesVec1.get(i)).isCloseTo(resultValuesExpected[0][i][0], offset(ref * EPS));
    }

    DoubleArray valuesVec2 = interp.interpolate(xValues, yValues[0], xKey[0]);
    for (int k = 0; k < keyLength; ++k) {
      final double ref = resultValuesExpected[k][0][0] == 0. ? 1. : Math.abs(resultValuesExpected[k][0][0]);
      assertThat(valuesVec2.get(k)).isCloseTo(resultValuesExpected[k][0][0], offset(ref * EPS));
    }

    DoubleMatrix valuesMat1 = interp.interpolate(xValues, yValues[0], xKey);
    for (int j = 0; j < keyDim; ++j) {
      for (int k = 0; k < keyLength; ++k) {
        final double ref = resultValuesExpected[k][0][j] == 0. ? 1. : Math.abs(resultValuesExpected[k][0][j]);
        assertThat(valuesMat1.get(j, k)).isCloseTo(resultValuesExpected[k][0][j], offset(ref * EPS));
      }
    }

    DoubleMatrix valuesMat2 = interp.interpolate(xValues, yValues, xKey[0]);
    for (int i = 0; i < yDim; ++i) {
      for (int k = 0; k < keyLength; ++k) {
        final double ref = resultValuesExpected[k][i][0] == 0. ? 1. : Math.abs(resultValuesExpected[k][i][0]);
        assertThat(valuesMat2.get(i, k)).isCloseTo(resultValuesExpected[k][i][0], offset(ref * EPS));
      }
    }

    DoubleMatrix[] valuesMat3 = interp.interpolate(xValues, yValues, xKey);
    for (int i = 0; i < yDim; ++i) {
      for (int j = 0; j < keyDim; ++j) {
        for (int k = 0; k < keyLength; ++k) {
          final double ref = resultValuesExpected[k][i][j] == 0. ? 1. : Math.abs(resultValuesExpected[k][i][j]);
          assertThat(valuesMat3[k].get(i, j)).isCloseTo(resultValuesExpected[k][i][j], offset(ref * EPS));
        }
      }
    }
  }

  /**
   * 
   */
  @Test
  public void nullKeyTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[] yValues = new double[] {1., 3., 4.};
    double[] xKey = null;

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues, xKey));

  }

  /**
   * 
   */
  @Test
  public void nullKeyMultiTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[][] yValues = new double[][] {{1., 3., 4.}, {2., 3., 1.}};
    double[] xKey = null;

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues, xKey));

  }

  /**
   * 
   */
  @Test
  public void nullKeyMatrixTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[] yValues = new double[] {1., 3., 4.};
    double[][] xKey = null;

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues, xKey));

  }

  /**
   * 
   */
  @Test
  public void nullKeyMatrixMultiTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[][] yValues = new double[][] {{1., 3., 4.}, {2., 3., 1.}};
    double[][] xKey = null;

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues, xKey));

  }

  /**
   * 
   */
  @Test
  public void infiniteOutputTest() {
    double[] xValues = new double[] {1.e-308, 2.e-308};
    double[] yValues = new double[] {1., 1.e308};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));

  }

  /**
   * 
   */
  @Test
  public void infiniteOutputMultiTest() {
    double[] xValues = new double[] {1.e-308, 2.e-308};
    double[][] yValues = new double[][] {{1., 1.e308}, {2., 1.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanOutputTest() {
    double[] xValues = new double[] {1., 2.e-308, 3.e-308, 4.};
    double[] yValues = new double[] {1., 2., 1.e308, 3.};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanOutputMultiTest() {
    double[] xValues = new double[] {1., 2.e-308, 3.e-308, 4.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void largeInterpolantsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[][] yValues = new double[][] {{2., 10., 2., 5.}, {1., 2., 10., 11.}};

    PiecewiseCubicHermiteSplineInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues[0], 1.e308));
  }

}
