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
 * Test.
 */
public class NaturalSplineInterpolatorTest {
  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  /**
   * 
   */
  @Test
  public void recov2ptsTest() {
    final double[] xValues = new double[] {1., 2.};
    final double[] yValues = new double[] {6., 1.};

    final int nIntervalsExp = 1;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {{0., 0., -5., 6.}};

    NaturalSplineInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

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
   * 
   */
  @Test
  public void recov4ptsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4};
    final double[] yValues = new double[] {6., 25. / 6., 10. / 3., 4.};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp =
        new double[][] {{1. / 6., 0., -2., 6.}, {1. / 6., 1. / 2., -3. / 2., 25. / 6.}, {-1. / 3., 1., 0., 10. / 3.}};

    PiecewisePolynomialInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

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
   * 
   */
  @Test
  public void nullXvaluesTest() {
    double[] xValues = null;
    double[] yValues = new double[] {1., 2., 3., 4.};

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void recov2ptsMultiTest() {
    final double[] xValues = new double[] {1., 2.};
    final double[][] yValues = new double[][] {{6., 1.}, {2., 5.}};

    final int nIntervalsExp = 1;
    final int orderExp = 4;
    final int dimExp = 2;
    final double[][] coefsMatExp = new double[][] {{0., 0., -5., 6.}, {0., 0., 3., 2.}};

    NaturalSplineInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

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
  public void recov4ptsMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4};
    final double[][] yValues = new double[][] {{6., 25. / 6., 10. / 3., 4.}, {6., 1., 0., 0.}};

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 2;
    final double[][] coefsMatExp = new double[][] {
        {1. / 6., 0., -2., 6.},
        {1., 0., -6., 6.},
        {1. / 6., 1. / 2., -3. / 2., 25. / 6.},
        {-1., 3., -3., 1.},
        {-1. / 3., 1., 0., 10. / 3.},
        {0., 0., 0., 0}};

    NaturalSplineInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues));
  }

  /**
   * Derive value of the underlying cubic spline function at the value of xKey
   */
  @Test
  public void interpolantsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[][] yValues = new double[][] {{6., 25. / 6., 10. / 3., 4.}, {6., 1., 0., 0.}};
    final double[][] xKey = new double[][] {{-1., 0.5, 1.5}, {2.5, 3.5, 4.5}};

    final double[][][] resultValuesExpected =
        new double[][][] {
            {{26. / 3., 57. / 16.}, {10., 1. / 8.}},
            {{335. / 48., 85. / 24.}, {71. / 8., 0.}},
            {{241. / 48., 107. / 24.}, {25. / 8., 0.}}};

    final int yDim = yValues.length;
    final int keyLength = xKey[0].length;
    final int keyDim = xKey.length;

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();

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
  public void infiniteOutputTest() {
    double[] xValues = new double[] {1.e-308, 2.e-308};
    double[] yValues = new double[] {1., 1.e308};

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues[0], 1.e308));
  }

  /**
   * 
   */
  @Test
  public void nullKeyTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[] yValues = new double[] {1., 3., 4.};
    double[] xKey = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

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

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> interp.interpolate(xValues, yValues, xKey));

  }
}
