/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
@Test
public class NaturalSplineInterpolatorTest {
  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  /**
   * 
   */
  public void recov2ptsTest() {
    final double[] xValues = new double[] {1., 2. };
    final double[] yValues = new double[] {6., 1. };

    final int nIntervalsExp = 1;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] {{0., 0., -5., 6. } };

    NaturalSplineInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

    assertEquals(result.getDimensions(), dimExp);
    assertEquals(result.getNumberOfIntervals(), nIntervalsExp);
    assertEquals(result.getDimensions(), dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertEquals(result.getCoefMatrix().get(i, j), coefsMatExp[i][j], ref * EPS);
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertEquals(result.getKnots().get(j), xValues[j]);
    }
  }

  /**
   * 
   */
  public void recov4ptsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4 };
    final double[] yValues = new double[] {6., 25. / 6., 10. / 3., 4. };

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 1;
    final double[][] coefsMatExp = new double[][] { {1. / 6., 0., -2., 6. }, {1. / 6., 1. / 2., -3. / 2., 25. / 6. }, {-1. / 3., 1., 0., 10. / 3. } };

    PiecewisePolynomialInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

    assertEquals(result.getDimensions(), dimExp);
    assertEquals(result.getNumberOfIntervals(), nIntervalsExp);
    assertEquals(result.getDimensions(), dimExp);

    for (int i = 0; i < nIntervalsExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertEquals(result.getCoefMatrix().get(i, j), coefsMatExp[i][j], ref * EPS);
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertEquals(result.getKnots().get(j), xValues[j]);
    }
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullXvaluesTest() {
    double[] xValues = new double[4];
    double[] yValues = new double[] {1., 2., 3., 4. };

    xValues = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullYvaluesTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[] yValues = new double[4];

    yValues = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void wrongDatalengthTest() {
    double[] xValues = new double[] {1., 2., 3. };
    double[] yValues = new double[] {1., 2., 3., 4. };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shortDataLengthTest() {
    double[] xValues = new double[] {1. };
    double[] yValues = new double[] {4. };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxValuesTest() {
    double[] xValues = new double[] {1., 2., Double.NaN, 4. };
    double[] yValues = new double[] {1., 2., 3., 4. };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNyValuesTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[] yValues = new double[] {1., 2., Double.NaN, 4. };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfxValuesTest() {
    double[] xValues = new double[] {1., 2., 3., INF };
    double[] yValues = new double[] {1., 2., 3., 4. };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfyValuesTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[] yValues = new double[] {1., 2., 3., INF };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void coincideXvaluesTest() {
    double[] xValues = new double[] {1., 2., 3., 3. };
    double[] yValues = new double[] {1., 2., 3., 4. };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  public void recov2ptsMultiTest() {
    final double[] xValues = new double[] {1., 2. };
    final double[][] yValues = new double[][] { {6., 1. }, {2., 5. } };

    final int nIntervalsExp = 1;
    final int orderExp = 4;
    final int dimExp = 2;
    final double[][] coefsMatExp = new double[][] { {0., 0., -5., 6. }, {0., 0., 3., 2. } };

    NaturalSplineInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

    assertEquals(result.getDimensions(), dimExp);
    assertEquals(result.getNumberOfIntervals(), nIntervalsExp);
    assertEquals(result.getDimensions(), dimExp);

    for (int i = 0; i < nIntervalsExp * dimExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertEquals(result.getCoefMatrix().get(i, j), coefsMatExp[i][j], ref * EPS);
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertEquals(result.getKnots().get(j), xValues[j]);
    }
  }

  /**
   * 
   */
  public void recov4ptsMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4 };
    final double[][] yValues = new double[][] { {6., 25. / 6., 10. / 3., 4. }, {6., 1., 0., 0. } };

    final int nIntervalsExp = 3;
    final int orderExp = 4;
    final int dimExp = 2;
    final double[][] coefsMatExp = new double[][] { {1. / 6., 0., -2., 6. }, {1., 0., -6., 6. }, {1. / 6., 1. / 2., -3. / 2., 25. / 6. }, {-1., 3., -3., 1. }, {-1. / 3., 1., 0., 10. / 3. },
      {0., 0., 0., 0 } };

    NaturalSplineInterpolator interpMatrix = new NaturalSplineInterpolator();

    PiecewisePolynomialResult result = interpMatrix.interpolate(xValues, yValues);

    assertEquals(result.getDimensions(), dimExp);
    assertEquals(result.getNumberOfIntervals(), nIntervalsExp);
    assertEquals(result.getDimensions(), dimExp);

    for (int i = 0; i < nIntervalsExp * dimExp; ++i) {
      for (int j = 0; j < orderExp; ++j) {
        final double ref = coefsMatExp[i][j] == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertEquals(result.getCoefMatrix().get(i, j), coefsMatExp[i][j], ref * EPS);
      }
    }

    for (int j = 0; j < nIntervalsExp + 1; ++j) {
      assertEquals(result.getKnots().get(j), xValues[j]);
    }
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullXvaluesMultiTest() {
    double[] xValues = new double[4];
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {1., 5., 3., 4. } };

    xValues = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullYvaluesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[][] yValues = new double[2][4];

    yValues = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void wrongDatalengthMultiTest() {
    double[] xValues = new double[] {1., 2., 3. };
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {2., 2., 3., 4. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shortDataLengthMultiTest() {
    double[] xValues = new double[] {1. };
    double[][] yValues = new double[][] { {4. }, {1. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxValuesMultiTest() {
    double[] xValues = new double[] {1., 2., Double.NaN, 4. };
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {2., 2., 3., 4. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNyValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {1., 2., Double.NaN, 4. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfxValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., INF };
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {2., 2., 3., 4. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfyValuesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {1., 2., 3., INF } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void coincideXvaluesMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 3. };
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {2., 2., 3., 4. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * Derive value of the underlying cubic spline function at the value of xKey
   */
  public void InterpolantsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {6., 25. / 6., 10. / 3., 4. }, {6., 1., 0., 0. } };
    final double[][] xKey = new double[][] { {-1., 0.5, 1.5 }, {2.5, 3.5, 4.5 } };

    final double[][][] resultValuesExpected = new double[][][] { { {26. / 3., 57. / 16. }, {10., 1. / 8. } }, { {335. / 48., 85. / 24. }, {71. / 8., 0. } },
      { {241. / 48., 107. / 24. }, {25. / 8., 0. } } };

    final int yDim = yValues.length;
    final int keyLength = xKey[0].length;
    final int keyDim = xKey.length;

    PiecewisePolynomialInterpolator interp = new NaturalSplineInterpolator();

    double value = interp.interpolate(xValues, yValues[0], xKey[0][0]);
    {
      final double ref = resultValuesExpected[0][0][0] == 0. ? 1. : Math.abs(resultValuesExpected[0][0][0]);
      assertEquals(value, resultValuesExpected[0][0][0], ref * EPS);
    }

    DoubleArray valuesVec1 = interp.interpolate(xValues, yValues, xKey[0][0]);
    for (int i = 0; i < yDim; ++i) {
      final double ref = resultValuesExpected[0][i][0] == 0. ? 1. : Math.abs(resultValuesExpected[0][i][0]);
      assertEquals(valuesVec1.get(i), resultValuesExpected[0][i][0], ref * EPS);
    }

    DoubleArray valuesVec2 = interp.interpolate(xValues, yValues[0], xKey[0]);
    for (int k = 0; k < keyLength; ++k) {
      final double ref = resultValuesExpected[k][0][0] == 0. ? 1. : Math.abs(resultValuesExpected[k][0][0]);
      assertEquals(valuesVec2.get(k), resultValuesExpected[k][0][0], ref * EPS);
    }

    DoubleMatrix valuesMat1 = interp.interpolate(xValues, yValues[0], xKey);
    for (int j = 0; j < keyDim; ++j) {
      for (int k = 0; k < keyLength; ++k) {
        final double ref = resultValuesExpected[k][0][j] == 0. ? 1. : Math.abs(resultValuesExpected[k][0][j]);
        assertEquals(valuesMat1.get(j, k), resultValuesExpected[k][0][j], ref * EPS);
      }
    }

    DoubleMatrix valuesMat2 = interp.interpolate(xValues, yValues, xKey[0]);
    for (int i = 0; i < yDim; ++i) {
      for (int k = 0; k < keyLength; ++k) {
        final double ref = resultValuesExpected[k][i][0] == 0. ? 1. : Math.abs(resultValuesExpected[k][i][0]);
        assertEquals(valuesMat2.get(i, k), resultValuesExpected[k][i][0], ref * EPS);
      }
    }

    DoubleMatrix[] valuesMat3 = interp.interpolate(xValues, yValues, xKey);
    for (int i = 0; i < yDim; ++i) {
      for (int j = 0; j < keyDim; ++j) {
        for (int k = 0; k < keyLength; ++k) {
          final double ref = resultValuesExpected[k][i][j] == 0. ? 1. : Math.abs(resultValuesExpected[k][i][j]);
          assertEquals(valuesMat3[k].get(i, j), resultValuesExpected[k][i][j], ref * EPS);
        }
      }
    }
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfiniteOutputTest() {
    double[] xValues = new double[] {1.e-308, 2.e-308 };
    double[] yValues = new double[] {1., 1.e308 };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfiniteOutputMultiTest() {
    double[] xValues = new double[] {1.e-308, 2.e-308 };
    double[][] yValues = new double[][] { {1., 1.e308 }, {2., 1. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NanOutputTest() {
    double[] xValues = new double[] {1., 2.e-308, 3.e-308, 4. };
    double[] yValues = new double[] {1., 2., 1.e308, 3. };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NanOutputMultiTest() {
    double[] xValues = new double[] {1., 2.e-308, 3.e-308, 4. };
    double[][] yValues = new double[][] { {1., 2., 3., 4. }, {2., 2., 3., 4. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeInterpolantsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {2., 10., 2., 5. }, {1., 2., 10., 11. } };

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues[0], 1.e308);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullKeyTest() {
    double[] xValues = new double[] {1., 2., 3. };
    double[] yValues = new double[] {1., 3., 4. };
    double[] xKey = new double[3];

    xKey = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues, xKey);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullKeyMultiTest() {
    double[] xValues = new double[] {1., 2., 3. };
    double[][] yValues = new double[][] { {1., 3., 4. }, {2., 3., 1. } };
    double[] xKey = new double[3];

    xKey = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues, xKey);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullKeyMatrixTest() {
    double[] xValues = new double[] {1., 2., 3. };
    double[] yValues = new double[] {1., 3., 4. };
    double[][] xKey = new double[3][3];

    xKey = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues, xKey);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullKeyMatrixMultiTest() {
    double[] xValues = new double[] {1., 2., 3. };
    double[][] yValues = new double[][] { {1., 3., 4. }, {2., 3., 1. } };
    double[][] xKey = new double[3][4];

    xKey = null;

    NaturalSplineInterpolator interp = new NaturalSplineInterpolator();

    interp.interpolate(xValues, yValues, xKey);

  }
}
