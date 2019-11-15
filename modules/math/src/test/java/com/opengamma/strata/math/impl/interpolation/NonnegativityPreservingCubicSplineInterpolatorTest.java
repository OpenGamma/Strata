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

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;

/**
 * Test.
 */
public class NonnegativityPreservingCubicSplineInterpolatorTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  /**
   * 
   */
  @Test
  public void positivityClampedTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5.};
    final double[] yValues = new double[] {0., 0.1, 1., 1., 20., 5., 0.};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nPts = 101;
    for (int i = 0; i < 101; ++i) {
      final double key = 1. + 4. / (nPts - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) >= 0.).isTrue();
    }

    final int nData = xValues.length;
    for (int i = 1; i < nData - 2; ++i) {
      final double tau = Math.signum(resultPos.getCoefMatrix().get(i, 3));
      assertThat(resultPos.getCoefMatrix().get(i, 2) * tau)
          .isGreaterThanOrEqualTo(-3. * yValues[i + 1] * tau / (xValues[i + 1] - xValues[i]));
      assertThat(resultPos.getCoefMatrix().get(i, 2) * tau)
          .isLessThanOrEqualTo(3. * yValues[i + 1] * tau / (xValues[i] - xValues[i - 1]));
    }
  }

  /**
   * 
   */
  @Test
  public void positivityClampedMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5.};
    final double[][] yValues = new double[][] {{0., 0.1, 1., 1., 20., 5., 0.}, {-10., 0.1, 1., 1., 20., 5., 0.}};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nPts = 101;
    for (int i = 0; i < 101; ++i) {
      final double key = 1. + 4. / (nPts - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) >= 0.).isTrue();
    }

    int dim = yValues.length;
    int nData = xValues.length;
    for (int j = 0; j < dim; ++j) {
      for (int i = 1; i < nData - 2; ++i) {
        DoubleMatrix coefMatrix = resultPos.getCoefMatrix();
        double tau = Math.signum(coefMatrix.get(dim * i + j, 3));
        assertThat(coefMatrix.get(dim * i + j, 2) * tau)
            .isGreaterThanOrEqualTo(-3. * yValues[j][i + 1] * tau / (xValues[i + 1] - xValues[i]));
        assertThat(coefMatrix.get(dim * i + j, 2) * tau)
            .isLessThanOrEqualTo(3. * yValues[j][i + 1] * tau / (xValues[i] - xValues[i - 1]));
      }
    }
  }

  /**
   * 
   */
  @Test
  public void positivityNotAKnotTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5.};
    final double[] yValues = new double[] {0.1, 1., 1., 20., 5.};

    PiecewisePolynomialInterpolator interp = new CubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nPts = 101;
    for (int i = 0; i < 101; ++i) {
      final double key = 1. + 4. / (nPts - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) >= 0.).isTrue();
    }

    final int nData = xValues.length;
    for (int i = 1; i < nData - 2; ++i) {
      final double tau = Math.signum(resultPos.getCoefMatrix().get(i, 3));
      assertThat(resultPos.getCoefMatrix().get(i, 2) * tau)
          .isGreaterThanOrEqualTo(-3. * yValues[i] * tau / (xValues[i + 1] - xValues[i]));
      assertThat(resultPos.getCoefMatrix().get(i, 2) * tau)
          .isLessThanOrEqualTo(3. * yValues[i] * tau / (xValues[i] - xValues[i - 1]));
    }
  }

  /**
   * 
   */
  @Test
  public void positivityEndIntervalsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6.};
    final double[][] yValues = new double[][] {{0.01, 0.01, 0.01, 10., 20., 1.}, {0.01, 0.01, 10., 10., 0.01, 0.01}};

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    final int nPts = 101;
    for (int i = 0; i < 101; ++i) {
      final double key = 1. + 5. / (nPts - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) >= 0.).isTrue();
    }

    int dim = yValues.length;
    int nData = xValues.length;
    for (int j = 0; j < dim; ++j) {
      for (int i = 1; i < nData - 2; ++i) {
        DoubleMatrix coefMatrix = resultPos.getCoefMatrix();
        double tau = Math.signum(coefMatrix.get(dim * i + j, 3));
        assertThat(coefMatrix.get(dim * i + j, 2) * tau)
            .isGreaterThanOrEqualTo(-3. * yValues[j][i] * tau / (xValues[i + 1] - xValues[i]));
        assertThat(coefMatrix.get(dim * i + j, 2) * tau)
            .isLessThanOrEqualTo(3. * yValues[j][i] * tau / (xValues[i] - xValues[i - 1]));
      }
    }
  }

  /**
   * PiecewiseCubicHermiteSplineInterpolator is not modified for positive data
   */
  @Test
  public void noModificationTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5.};
    final double[][] yValues = new double[][] {{0.1, 1., 1., 20., 5.}, {1., 2., 3., 0., 0.}};

    PiecewisePolynomialInterpolator interp = new PiecewiseCubicHermiteSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);

    assertThat(resultPos.getDimensions()).isEqualTo(result.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(result.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(result.getOrder());

    for (int i = 1; i < xValues.length - 1; ++i) {
      for (int j = 0; j < 4; ++j) {
        final double ref = result.getCoefMatrix().get(i, j) == 0. ? 1. : Math.abs(result.getCoefMatrix().get(i, j));
        assertThat(resultPos.getCoefMatrix().get(i, j)).isCloseTo(result.getCoefMatrix().get(i, j), offset(ref * EPS));
      }
    }
  }

  /**
   * 
   */
  @Test
  public void flipTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6.};
    final double[] yValues = new double[] {3., 0.1, 0.01, 0.01, 0.1, 3.};

    final double[] xValuesFlip = new double[] {6., 2., 3., 5., 4., 1.};
    final double[] yValuesFlip = new double[] {3., 0.1, 0.01, 0.1, 0.01, 3.};

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);
    PiecewisePolynomialResult resultPosFlip = interpPos.interpolate(xValuesFlip, yValuesFlip);

    assertThat(resultPos.getDimensions()).isEqualTo(resultPosFlip.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(resultPosFlip.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(resultPosFlip.getOrder());

    final int nPts = 101;
    for (int i = 0; i < 101; ++i) {
      final double key = 1. + 5. / (nPts - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0) >= 0.).isTrue();
    }

    final int nData = xValues.length;
    for (int i = 0; i < nData - 1; ++i) {
      for (int k = 0; k < 4; ++k) {
        assertThat(resultPos.getCoefMatrix().get(i, k)).isEqualTo(resultPosFlip.getCoefMatrix().get(i, k));
      }
    }
  }

  /**
   * 
   */
  @Test
  public void flipMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6.};
    final double[][] yValues = new double[][] {{3., 0.1, 0.01, 0.01, 0.1, 3.}, {3., 0.1, 0.01, 0.001, 2., 3.}};

    final double[] xValuesFlip = new double[] {1., 2., 3., 5., 4., 6.};
    final double[][] yValuesFlip = new double[][] {{3., 0.1, 0.01, 0.1, 0.01, 3.}, {3., 0.1, 0.01, 2., 0.001, 3.}};

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    PiecewisePolynomialResult resultPos = interpPos.interpolate(xValues, yValues);
    PiecewisePolynomialResult resultPosFlip = interpPos.interpolate(xValuesFlip, yValuesFlip);

    assertThat(resultPos.getDimensions()).isEqualTo(resultPosFlip.getDimensions());
    assertThat(resultPos.getNumberOfIntervals()).isEqualTo(resultPosFlip.getNumberOfIntervals());
    assertThat(resultPos.getOrder()).isEqualTo(resultPosFlip.getOrder());

    final int nPts = 101;
    for (int i = 0; i < 101; ++i) {
      final double key = 1. + 5. / (nPts - 1) * i;
      assertThat(function.evaluate(resultPos, key).get(0)).isGreaterThanOrEqualTo(0.);
      assertThat(function.evaluate(resultPos, key).get(1)).isGreaterThanOrEqualTo(0.);
    }

    int dim = yValues.length;
    int nData = xValues.length;
    for (int j = 0; j < dim; ++j) {
      for (int i = 0; i < nData - 1; ++i) {
        for (int k = 0; k < 4; ++k) {
          assertThat(resultPos.getCoefMatrix().get(dim * i + j, k))
              .isEqualTo(resultPosFlip.getCoefMatrix().get(dim * i + j, k));
        }
      }
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
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
    PiecewisePolynomialInterpolator interpPos = new NonnegativityPreservingCubicSplineInterpolator(interp);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

}
