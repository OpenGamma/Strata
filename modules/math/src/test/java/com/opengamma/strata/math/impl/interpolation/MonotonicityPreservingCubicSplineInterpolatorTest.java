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

import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;

/**
 * Test.
 */
public class MonotonicityPreservingCubicSplineInterpolatorTest {

  private static final double EPS = 1e-13;
  private static final double INF = 1. / 0.;

  /**
   * 
   */
  @Test
  public void localMonotonicityIncTest() {
    final double[] xValues = new double[] {2., 3., 5., 8., 9., 13.};
    final double[] yValues = new double[] {1., 1.01, 2., 2.1, 2.2, 2.201};

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nKeys = 111;
    double key0 = 2.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 2. + 11. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) - function.evaluate(resultPos, key0).get(0) >= 0.).isTrue();

      key0 = 2. + 11. / (nKeys - 1) * i;
    }
  }

  /**
   * 
   */
  @Test
  public void localMonotonicityClampedTest() {
    final double[] xValues = new double[] {-2., 3., 4., 8., 9.1, 10.};
    final double[] yValues = new double[] {0., 10., 9.5, 2., 1.1, -2.2, -2.6, 0.};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nKeys = 121;
    double key0 = -2.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = -2. + 12. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) - function.evaluate(resultPos, key0).get(0) <= 0.).isTrue();

      key0 = -2. + 11. / (nKeys - 1) * i;
    }
  }

  /**
   * 
   */
  @Test
  public void localMonotonicityClampedMultiTest() {
    final double[] xValues = new double[] {-2., 3., 4., 8., 9.1, 10.};
    final double[][] yValues =
        new double[][] {{0., 10., 9.5, 2., 1.1, -2.2, -2.6, 0.}, {10., 10., 9.5, 2., 1.1, -2.2, -2.6, 10.}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nKeys = 121;
    double key0 = -2.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = -2. + 12. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) - function.evaluate(resultPos, key0).get(0) <= 0.).isTrue();

      key0 = -2. + 11. / (nKeys - 1) * i;
    }
    key0 = -2.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = -2. + 12. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(1) - function.evaluate(resultPos, key0).get(1) <= 0.).isTrue();

      key0 = -2. + 11. / (nKeys - 1) * i;
    }
  }

  /**
   * 
   */
  @Test
  public void localMonotonicityDecTest() {
    final double[] xValues = new double[] {-2., 3., 4., 8., 9.1, 10.};
    final double[] yValues = new double[] {10., 9.5, 2., 1.1, -2.2, -2.6};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nKeys = 121;
    double key0 = -2.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = -2. + 12. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) - function.evaluate(resultPos, key0).get(0) <= 0.).isTrue();

      key0 = -2. + 11. / (nKeys - 1) * i;
    }
  }

  /**
   * local extrema are not necessarily at data-points
   */
  @Test
  public void extremumTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8};
    final double[][] yValues = new double[][] {{1., 1., 2., 4., 4., 2., 1., 1.}, {10., 10., 6., 4., 4., 6., 10., 10.}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    assertThat(function.evaluate(resultPos, 4.5).get(0) - function.evaluate(resultPos, 4).get(0) >= 0.).isTrue();
    assertThat(function.evaluate(resultPos, 4.5).get(0) - function.evaluate(resultPos, 5).get(0) >= 0.).isTrue();
    assertThat(function.evaluate(resultPos, 4.5).get(1) - function.evaluate(resultPos, 4).get(1) <= 0.).isTrue();
    assertThat(function.evaluate(resultPos, 4.5).get(1) - function.evaluate(resultPos, 5).get(1) <= 0.).isTrue();

    final int nKeys = 41;
    double key0 = 1.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 1. + 3. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) - function.evaluate(resultPos, key0).get(0) >= 0.).isTrue();

      key0 = 1. + 3. / (nKeys - 1) * i;
    }
    key0 = 1.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 1. + 3. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(1) - function.evaluate(resultPos, key0).get(1) <= 0.).isTrue();

      key0 = 1. + 3. / (nKeys - 1) * i;
    }
    key0 = 5.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 5. + 3. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) - function.evaluate(resultPos, key0).get(0) <= 0.).isTrue();

      key0 = 5. + 3. / (nKeys - 1) * i;
    }
    key0 = 5.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 5. + 3. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(1) - function.evaluate(resultPos, key0).get(1) >= 0.).isTrue();

      key0 = 5. + 3. / (nKeys - 1) * i;
    }
  }

  /**
   * PiecewiseCubicHermiteSplineInterpolator is not modified except the first 2 and last 2 intervals
   */
  @Test
  public void localMonotonicityDec2Test() {
    final double[] xValues = new double[] {-2., 3., 4., 8., 9.1, 10., 12., 14.};
    final double[] yValues = new double[] {11., 9.5, 2., 1.1, -2.2, -2.6, 2., 2.};

    PiecewisePolynomialInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    for (int i = 2; i < resultPos.getNumberOfIntervals() - 2; ++i) {
      for (int j = 0; j < 4; ++j) {
        assertThat(resultPos.getCoefMatrix().get(i, j)).isCloseTo(result.getCoefMatrix().get(i, j), offset(EPS));
      }
    }

    final int nKeys = 121;
    double key0 = -2.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = -2. + 12. / (nKeys - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) - function.evaluate(resultPos, key0).get(0) <= 0.).isTrue();

      key0 = -2. + 11. / (nKeys - 1) * i;
    }
  }

  /*
   * Error tests
   */
  /**
   * Primary interpolation method should be cubic. 
   * Note that CubicSplineInterpolator returns a linear or quadratic function in certain situations 
   */
  @Test
  public void lowDegreeTest() {
    final double[] xValues = new double[] {1., 2., 3.};
    final double[] yValues = new double[] {0., 0.1, 0.05};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void lowDegreeMultiTest() {
    final double[] xValues = new double[] {1., 2., 3.};
    final double[][] yValues = new double[][] {{0., 0.1, 0.05}, {0., 0.1, 1.05}};

    PiecewisePolynomialInterpolator interp = new LinearInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void dataShortTest() {
    final double[] xValues = new double[] {1., 2.};
    final double[] yValues = new double[] {0., 0.1};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void dataShortMultiTest() {
    final double[] xValues = new double[] {1., 2.,};
    final double[][] yValues = new double[][] {{0., 0.1}, {0., 0.1}};

    PiecewisePolynomialInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void coincideDataTest() {
    final double[] xValues = new double[] {1., 1., 3.};
    final double[] yValues = new double[] {0., 0.1, 0.05};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void coincideDataMultiTest() {
    final double[] xValues = new double[] {1., 2., 2.};
    final double[][] yValues = new double[][] {{2., 0., 0.1, 0.05, 2.}, {1., 0., 0.1, 1.05, 2.}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void diffDataTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[] yValues = new double[] {0., 0.1, 0.05};

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void diffDataMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[][] yValues = new double[][] {{2., 0., 0.1, 0.05, 2.}, {1., 0., 0.1, 1.05, 2.}};

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullXdataTest() {
    double[] xValues = null;
    double[] yValues = new double[] {0., 0.1, 0.05, 0.2};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = null;

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullXdataMultiTest() {
    double[] xValues = null;
    double[][] yValues = new double[][] {{0., 0.1, 0.05, 0.2}, {0., 0.1, 0.05, 0.2}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = null;

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infXdataTest() {
    double[] xValues = new double[] {1., 2., 3., INF};
    double[] yValues = new double[] {0., 0.1, 0.05, 0.2};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {0., 0., 0.1, 0.05, 0.2, INF};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanXdataTest() {
    double[] xValues = new double[] {1., 2., 3., Double.NaN};
    double[] yValues = new double[] {0., 0.1, 0.05, 0.2};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {0., 0., 0.1, 0.05, 0.2, Double.NaN};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infXdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., INF};
    double[][] yValues = new double[][] {{0., 0.1, 0.05, 0.2}, {0., 0.1, 0.05, 0.2}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{0., 0., 0.1, 0.05, 0.2, 1.}, {0., 0., 0.1, 0.05, 0.2, INF}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanXdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., Double.NaN};
    double[][] yValues = new double[][] {{0., 0.1, 0.05, 0.2}, {0., 0.1, 0.05, 0.2}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{0., 0., 0.1, 0.05, 0.2, 1.1}, {0., 0., 0.1, 0.05, 0.2, Double.NaN}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator interpPos = new MonotonicityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }
}
