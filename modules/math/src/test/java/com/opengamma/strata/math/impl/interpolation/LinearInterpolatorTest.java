/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;

/**
 * Test.
 */
public class LinearInterpolatorTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;
  private static final LinearInterpolator INTERP = new LinearInterpolator();

  /**
   * 
   */
  @Test
  public void recov2ptsTest() {
    double[] xValues = new double[] {1., 2.};
    double[] yValues = new double[] {6., 1.};

    int nIntervalsExp = 1;
    int orderExp = 2;
    int dimExp = 1;
    double[][] coefsMatExp = new double[][] {{-5., 6.}};
    PiecewisePolynomialResult result = INTERP.interpolate(xValues, yValues);
    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }
    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }

    // sensitivity
    double delta = 1.0e-6;
    double[] keys = new double[] {-1.2, 1.63, 2.3};
    testSensitivity(xValues, yValues, keys, delta);
  }

  /**
   * 
   */
  @Test
  public void recov4ptsTest() {
    double[] xValues = new double[] {1., 2., 4., 7.};
    double[] yValues = new double[] {6., 1., 8., -2.};

    int nIntervalsExp = 3;
    int orderExp = 2;
    int dimExp = 1;
    double[][] coefsMatExp = new double[][] {{-5., 6.}, {7. / 2., 1.}, {-10. / 3., 8.}};
    LinearInterpolator interpMatrix = new LinearInterpolator();
    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);
    assertThat(result.getDimensions()).isEqualTo(dimExp);
    assertThat(result.getNumberOfIntervals()).isEqualTo(nIntervalsExp);
    assertThat(result.getDimensions()).isEqualTo(dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertThat(result.getCoefMatrix().get(i, j)).isCloseTo(coefsMatExp[i][j], offset(ref * EPS));
      }
    }
    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertThat(result.getKnots().get(j)).isEqualTo(xValues[j]);
    }

    // sensitivity
    double delta = 1.0e-6;
    double[] keys = new double[] {-1.5, 2.43, 4.0, 7.0, 12.7};
    testSensitivity(xValues, yValues, keys, delta);
  }

  /**
   * 
   */
  @Test
  public void nullXvaluesTest() {
    double[] xValues = null;
    double[] yValues = new double[] {1., 2., 3., 4.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullYvaluesTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = null;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void wrongDatalengthTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[] yValues = new double[] {1., 2., 3., 4.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void shortDataLengthTest() {
    double[] xValues = new double[] {1.};
    double[] yValues = new double[] {4.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNxValuesTest() {
    double[] xValues = new double[] {1., 2., Double.NaN, 4.};
    double[] yValues = new double[] {1., 2., 3., 4.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNyValuesTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {1., 2., Double.NaN, 4.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infxValuesTest() {
    double[] xValues = new double[] {1., 2., 3., INF};
    double[] yValues = new double[] {1., 2., 3., 4.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infyValuesTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[] yValues = new double[] {1., 2., 3., INF};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void coincideXvaluesTest() {
    double[] xValues = new double[] {1., 2., 3., 3.};
    double[] yValues = new double[] {1., 2., 3., 4.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void recov2ptsMultiTest() {
    final double[] xValues = new double[] {1., 2.};
    final double[][] yValues = new double[][] {{6., 1.}, {2., 5.}};

    final int nIntervalsExp = 1;
    final int orderExp = 2;
    final int dimExp = 2;
    final double[][] coefsMatExp = new double[][] {{-5., 6.}, {3., 2.}};
    LinearInterpolator interpMatrix = new LinearInterpolator();
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
    final double[][] yValues = new double[][] {{6., 1., 8., -2.}, {1., 1. / 3., 2. / 11., 1. / 7.}};

    final int nIntervalsExp = 3;
    final int orderExp = 2;
    final int dimExp = 2;
    final double[][] coefsMatExp =
        new double[][] {{-5., 6.}, {-2. / 3., 1.}, {7., 1.}, {-5. / 33., 1. / 3.}, {-10., 8.}, {-3. / 77., 2. / 11.}};
    LinearInterpolator interpMatrix = new LinearInterpolator();
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void nullYvaluesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = null;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void wrongDatalengthMultiTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void shortDataLengthMultiTest() {
    double[] xValues = new double[] {1.};
    double[][] yValues = new double[][] {{4.}, {1.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNxValuesMultiTest() {
    double[] xValues = new double[] {1., 2., Double.NaN, 4.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void naNyValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {1., 2., Double.NaN, 4.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infxValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., INF};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void infyValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {1., 2., 3., INF}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void coincideXvaluesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 3.};
    double[][] yValues = new double[][] {{1., 2., 3., 4.}, {2., 2., 3., 4.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
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
        new double[][][] {{{29. / 3., 15. / 4.}, {16., 1. / 2.}}, {{83. / 12., 11. / 3.}, {17. / 2., 0.}},
            {{61. / 12., 13. / 3.}, {7. / 2., 0.}}};

    final int yDim = yValues.length;
    final int keyLength = xKey[0].length;
    final int keyDim = xKey.length;

    LinearInterpolator interp = new LinearInterpolator();

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
  public void largeOutputTest() {
    double[] xValues = new double[] {1., 2.e-308, 3.e-308, 4.};
    double[] yValues = new double[] {1., 2., 1.e308, 3.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void largeOutputMultiTest() {
    double[] xValues = new double[] {1., 2.e-308, 3.e-308, 4.};
    double[][] yValues = new double[][] {{1., 2.e307, 3., 4.}, {2., 2., 3., 4.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void largeInterpolantsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4.};
    final double[][] yValues = new double[][] {{2., 10., 2., 5.}, {1., 2., 10., 11.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues[0], 1.e308));
  }

  /**
   * 
   */
  @Test
  public void nullKeyTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[] yValues = new double[] {1., 3., 4.};
    double[] xKey = null;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues, xKey));
  }

  /**
   * 
   */
  @Test
  public void nullKeyMultiTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[][] yValues = new double[][] {{1., 3., 4.}, {2., 3., 1.}};
    double[] xKey = null;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues, xKey));
  }

  /**
   * 
   */
  @Test
  public void nullKeyMatrixTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[] yValues = new double[] {1., 3., 4.};
    double[][] xKey = null;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues, xKey));
  }

  /**
   * 
   */
  @Test
  public void nullKeyMatrixMultiTest() {
    double[] xValues = new double[] {1., 2., 3.};
    double[][] yValues = new double[][] {{1., 3., 4.}, {2., 3., 1.}};
    double[][] xKey = null;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTERP.interpolate(xValues, yValues, xKey));
  }

  /**
   * 
   */
  @Test
  public void notReconnectedTest() {
    double[] xValues = new double[] {1., 2.000000000001, 2.000000000002, 4.};
    double[] yValues = new double[] {2., 4.e10, 3.e-5, 5.e11};

    PiecewisePolynomialInterpolator interpPos = new LinearInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  /**
   * 
   */
  @Test
  public void notReconnectedMultiTest() {
    double[] xValues = new double[] {1., 2.000000000001, 2.000000000002, 4.};
    double[][] yValues = new double[][] {{2., 4.e10, 3.e-5, 5.e11}};

    PiecewisePolynomialInterpolator interpPos = new LinearInterpolator();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> interpPos.interpolate(xValues, yValues));
  }

  //-------------------------------------------------------------------------
  private void testSensitivity(double[] xValues, double[] yValues, double[] keys, double delta) {
    PiecewisePolynomialWithSensitivityFunction1D func = new PiecewisePolynomialWithSensitivityFunction1D();
    PiecewisePolynomialResultsWithSensitivity resultSensi = INTERP.interpolateWithSensitivity(xValues, yValues);
    DoubleArray[] computedArray = func.nodeSensitivity(resultSensi, keys);
    for (int i = 0; i < keys.length; ++i) {
      double base = func.evaluate(resultSensi, keys[i]).get(0);
      DoubleArray computed = func.nodeSensitivity(resultSensi, keys[i]);
      assertThat(computed).isEqualTo(computedArray[i]);
      for (int j = 0; j < yValues.length; ++j) {
        double[] yValuesBump = Arrays.copyOf(yValues, yValues.length);
        yValuesBump[j] += delta;
        PiecewisePolynomialResult resultBump = INTERP.interpolate(xValues, yValuesBump);
        double expected = (func.evaluate(resultBump, keys[i]).get(0) - base) / delta;
        assertThat(computed.get(j)).isCloseTo(expected, offset(delta));
      }
    }
  }
}
