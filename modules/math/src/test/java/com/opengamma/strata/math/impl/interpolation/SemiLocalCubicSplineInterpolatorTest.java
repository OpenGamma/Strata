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
public class SemiLocalCubicSplineInterpolatorTest {

  private static final double EPS = 1e-13;
  private static final double INF = 1. / 0.;

  /**
   * Recovering linear test
   */
  @Test
  public void linearTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6.};
    final int nData = xValues.length;
    final double[] yValues = new double[nData];
    for (int i = 0; i < nData; ++i) {
      yValues[i] = xValues[i] / 7. + 1 / 11.;
    }

    final double[][] coefsMatExp = new double[][] {
        {0., 0., 1. / 7., yValues[0]},
        {0., 0., 1. / 7., yValues[1]},
        {0., 0., 1. / 7., yValues[2]},
        {0., 0., 1. / 7., yValues[3]},
        {0., 0., 1. / 7., yValues[4]}};

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interp = new SemiLocalCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(1);
    assertThat(result.getNumberOfIntervals()).isEqualTo(5);
    assertThat(result.getOrder()).isEqualTo(4);

    for (int i = 0; i < result.getNumberOfIntervals(); ++i) {
      for (int j = 0; j < result.getOrder(); ++j) {
        final double ref = Math.abs(coefsMatExp[i][j]) == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    final int nKeys = 101;
    for (int i = 0; i < nKeys; ++i) {
      final double key = 1. + 5. / (nKeys - 1) * i;
      final double ref = key / 7. + 1 / 11.;
      assertThat(function.evaluate(result, key).get(0)).isCloseTo(ref, offset(ref * EPS));
    }
  }

  /**
   * Recovering quadratic function
   */
  @Test
  public void quadraticTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6.};
    final int nData = xValues.length;
    final double[] yValues = new double[nData];
    for (int i = 0; i < nData; ++i) {
      yValues[i] = xValues[i] * xValues[i] / 7. + xValues[i] / 13. + 1 / 11.;
    }

    final double[][] coefsMatExp = new double[][] {
        {0., 1. / 7., 2. / 7. * xValues[0] + 1. / 13., yValues[0]},
        {0., 1. / 7., 2. / 7. * xValues[1] + 1. / 13., yValues[1]},
        {0., 1. / 7., 2. / 7. * xValues[2] + 1. / 13., yValues[2]},
        {0., 1. / 7., 2. / 7. * xValues[3] + 1. / 13., yValues[3]},
        {0., 1. / 7., 2. / 7. * xValues[4] + 1. / 13., yValues[4]}};

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interp = new SemiLocalCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(1);
    assertThat(result.getNumberOfIntervals()).isEqualTo(5);
    assertThat(result.getOrder()).isEqualTo(4);

    for (int i = 0; i < result.getNumberOfIntervals(); ++i) {
      for (int j = 0; j < result.getOrder(); ++j) {
        final double ref = Math.abs(coefsMatExp[i][j]) == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    final int nKeys = 101;
    for (int i = 0; i < nKeys; ++i) {
      final double key = 1. + 5. / (nKeys - 1) * i;
      final double ref = key * key / 7. + key / 13. + 1 / 11.;
      assertThat(function.evaluate(result, key).get(0)).isCloseTo(ref, offset(ref * EPS));

    }
  }

  /**
   * 
   */
  @Test
  public void quadraticMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6.};
    final int nData = xValues.length;
    final double[][] yValues = new double[2][nData];
    for (int i = 0; i < nData; ++i) {
      yValues[0][i] = xValues[i] * xValues[i] / 7. + xValues[i] / 13. + 1 / 11.;
    }
    for (int i = 0; i < nData; ++i) {
      yValues[1][i] = xValues[i] * xValues[i] / 3. + xValues[i] / 7. + 1 / 17.;
    }

    final double[][] coefsMatExp = new double[][] {
        {0., 1. / 7., 2. / 7. * xValues[0] + 1. / 13., yValues[0][0]},
        {0., 1. / 3., 2. / 3. * xValues[0] + 1. / 7., yValues[1][0]},
        {0., 1. / 7., 2. / 7. * xValues[1] + 1. / 13., yValues[0][1]},
        {0., 1. / 3., 2. / 3. * xValues[1] + 1. / 7., yValues[1][1]},
        {0., 1. / 7., 2. / 7. * xValues[2] + 1. / 13., yValues[0][2]},
        {0., 1. / 3., 2. / 3. * xValues[2] + 1. / 7., yValues[1][2]},
        {0., 1. / 7., 2. / 7. * xValues[3] + 1. / 13., yValues[0][3]},
        {0., 1. / 3., 2. / 3. * xValues[3] + 1. / 7., yValues[1][3]},
        {0., 1. / 7., 2. / 7. * xValues[4] + 1. / 13., yValues[0][4]},
        {0., 1. / 3., 2. / 3. * xValues[4] + 1. / 7., yValues[1][4]}};

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interp = new SemiLocalCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(2);
    assertThat(result.getNumberOfIntervals()).isEqualTo(5);
    assertThat(result.getOrder()).isEqualTo(4);

    for (int i = 0; i < result.getNumberOfIntervals() * 2; ++i) {
      for (int j = 0; j < result.getOrder(); ++j) {
        final double ref = Math.abs(coefsMatExp[i][j]) == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }

    final int nKeys = 101;
    for (int i = 0; i < nKeys; ++i) {
      final double key = 1. + 5. / (nKeys - 1) * i;
      final double ref = key * key / 7. + key / 13. + 1 / 11.;
      assertThat(function.evaluate(result, key).get(0)).isCloseTo(ref, offset(ref * EPS));

    }
  }

  /**
   * Sample data given in the original paper, consisting of constant part and monotonically increasing part
   */
  @Test
  public void sampleDataTest() {
    final double[] xValues = new double[] {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10.};
    final double[] yValues = new double[] {10., 10., 10., 10., 10., 10., 10.5, 15., 50., 60., 85.};

    final double[][] coefsMatPartExp =
        new double[][] {{0., 0., 0., 10.}, {0., 0., 0., 10.}, {0., 0., 0., 10.}, {0., 0., 0., 10.}, {0., 0., 0., 10.}};

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interp = new SemiLocalCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertThat(result.getDimensions()).isEqualTo(1);
    assertThat(result.getNumberOfIntervals()).isEqualTo(10);
    assertThat(result.getOrder()).isEqualTo(4);

    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        final double ref = Math.abs(coefsMatPartExp[i][j]) == 0. ? 1. : Math.abs(coefsMatPartExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatPartExp[i][j], offset(ref * EPS));
      }
    }

    final int nKeys = 101;
    double key0 = 5.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 5. + 5. / (nKeys - 1) * i;
      assertThat(function.evaluate(result, key).get(0) - function.evaluate(result, key0).get(0) >= 0.).isTrue();
      key0 = 5. + 5. / (nKeys - 1) * i;
    }

    key0 = 0.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 0. + 5. / (nKeys - 1) * i;
      assertThat(function.evaluate(result, key).get(0) - function.evaluate(result, key0).get(0) == 0.).isTrue();
      key0 = 0. + 5. / (nKeys - 1) * i;
    }

  }

  /*
   * Error tests
   */

  /**
   * 
   */
  @Test
  public void dataShortTest() {
    final double[] xValues = new double[] {1., 2.};
    final double[] yValues = new double[] {0., 0.1};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void dataShortMultiTest() {
    final double[] xValues = new double[] {1., 2.,};
    final double[][] yValues = new double[][] {{0., 0.1,}, {0., 0.1,}};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void dataDiffTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[] yValues = new double[] {0., 0.1, 3.};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void dataDiffMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[][] yValues = new double[][] {{0., 0.1, 3.}, {0., 0.1, 3.}};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void coincideDataMultiTest() {
    final double[] xValues = new double[] {1., 2., 2.};
    final double[][] yValues = new double[][] {{0., 0.1, 0.05}, {0., 0.1, 1.05}};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {0.1, 0.05, 0.2, INF};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {0.1, 0.05, 0.2, Double.NaN};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{0.1, 0.05, 0.2, 1.}, {0.1, 0.05, 0.2, INF}};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
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

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nanYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{0.1, 0.05, 0.2, 1.1}, {0.1, 0.05, 0.2, Double.NaN}};

    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void notReconnectedTest() {
    double[] xValues = new double[] {1., 2.0000000001, 2., 4.};
    double[] yValues = new double[] {2., 400., 3., 500000000.};
    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void notReconnectedMultiTest() {
    double[] xValues = new double[] {1., 2., 4., 2.0000000001};
    double[][] yValues = new double[][] {{2., 3., 500000000., 400.}};
    PiecewisePolynomialInterpolator interpPos = new SemiLocalCubicSplineInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

}
