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
public class CubicSplineInterpolatorTest {
  private static final double EPS = 1e-14;

  /**
   * All of the recovery tests for normal values with Clamped endpoint condition
   */
  public void ClampedRecoverTest() {

    final double[] xValues = new double[] {1, 3, 2, 4 };
    final double[] yValues = new double[] {3, -1, 1, 0, 8, 12 };
    final double[][] yValuesMatrix = new double[][] { {3, -1, 1, 0, 8, 12 }, {-20, 20, 0, 5, 5, 10 } };

    final double[] xSamples = new double[] {0, 5. / 2., 5. / 3., 29. / 7. };
    final double[][] ySamples = new double[][] { {-8, 1. / 8., -1. / 27., Math.pow(15. / 7., 3.) }, {45., 5. / 4., 80. / 9., 320. / 49. } };

    final double[][] xSamplesMatrix = new double[][] { {-2, 1, 2, 3 }, {4, 5, 6, 7 } };
    final double[][][] ySamplesMatrix = new double[][][] { { {-64., -1., 0., 1. }, {8., 27., 64., 125. } }, { {125., 20., 5., 0 }, {5., 20., 45., 80. } } };
    final int xDim = 2;

    final DoubleMatrix coefsExpectedMatrix = DoubleMatrix.copyOf(
        new double[][] {
            {1., -3., 3., -1},
            {0., 5., -20., 20},
            {1., 0., 0., 0.},
            {0., 5., -10., 5},
            {1., 3., 3., 1.},
            {0., 5., 0., 0.}});
    final int dimMatrix = 2;
    final int orderMatrix = 4;
    final int nIntervalsMatrix = 3;
    final double[] knotsMatrix = new double[] {1, 2, 3, 4 };

    CubicSplineInterpolator interpMatrix = new CubicSplineInterpolator();

    PiecewisePolynomialResult resultMatrix = interpMatrix.interpolate(xValues, yValuesMatrix);

    final int nRows = coefsExpectedMatrix.rowCount();
    final int nCols = coefsExpectedMatrix.columnCount();
    for (int i = 0; i < nRows; ++i) {
      for (int j = 0; j < nCols; ++j) {
        final double ref = coefsExpectedMatrix.get(i, j) == 0. ? 1. : Math.abs(coefsExpectedMatrix.get(i, j));
        assertEquals(resultMatrix.getCoefMatrix().get(i, j), coefsExpectedMatrix.get(i, j), ref * EPS);
      }
    }

    assertEquals(resultMatrix.getNumberOfIntervals(), nIntervalsMatrix);
    assertEquals(resultMatrix.getOrder(), orderMatrix);
    assertEquals(resultMatrix.getDimensions(), dimMatrix);
    assertEquals(resultMatrix.getKnots().toArray(), knotsMatrix);

    DoubleMatrix resultValuesMatrix2D = interpMatrix.interpolate(xValues, yValuesMatrix, xSamples);
    final int nSamples = xSamples.length;
    for (int i = 0; i < dimMatrix; ++i) {
      for (int j = 0; j < nSamples; ++j) {
        final double ref = ySamples[i][j] == 0. ? 1. : Math.abs(ySamples[i][j]);
        assertEquals(resultValuesMatrix2D.get(i, j), ySamples[i][j], ref * EPS);
      }
    }

    DoubleArray resultValuesMatrix1D = interpMatrix.interpolate(xValues, yValuesMatrix, xSamples[0]);
    for (int i = 0; i < dimMatrix; ++i) {
      final double ref = ySamples[i][0] == 0. ? 1. : Math.abs(ySamples[i][0]);
      assertEquals(resultValuesMatrix1D.get(i), ySamples[i][0], ref * EPS);
    }

    DoubleMatrix[] resultValuesMatrix2DVec = interpMatrix.interpolate(xValues, yValuesMatrix, xSamplesMatrix);
    for (int i = 0; i < nSamples; ++i) {
      for (int j = 0; j < dimMatrix; ++j) {
        for (int k = 0; k < xDim; ++k) {
          double ref = ySamplesMatrix[j][k][i] == 0. ? 1. : Math.abs(ySamplesMatrix[j][k][i]);
          assertEquals(resultValuesMatrix2DVec[i].get(j, k), ySamplesMatrix[j][k][i], ref * EPS);
        }
      }
    }

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    for (int i = 0; i < nIntervalsMatrix; ++i) {
      for (int j = 0; j < nCols; ++j) {
        double ref = coefsExpectedMatrix.get(2 * i, j) == 0. ? 1. : Math.abs(coefsExpectedMatrix.get(2 * i, j));
        assertEquals(result.getCoefMatrix().get(i, j), coefsExpectedMatrix.get(2 * i, j), ref * EPS);
      }
    }

    assertEquals(result.getNumberOfIntervals(), nIntervalsMatrix);
    assertEquals(result.getOrder(), orderMatrix);
    assertEquals(result.getDimensions(), 1);
    assertEquals(result.getKnots().toArray(), knotsMatrix);

    DoubleArray resultValues1D = interp.interpolate(xValues, yValues, xSamples);
    for (int j = 0; j < nSamples; ++j) {
      final double ref = ySamples[0][j] == 0. ? 1. : Math.abs(ySamples[0][j]);
      assertEquals(resultValues1D.get(j), ySamples[0][j], ref * EPS);
    }

    double resultValue = interp.interpolate(xValues, yValues, xSamples[1]);
    final double refb = ySamples[0][1] == 0. ? 1. : Math.abs(ySamples[0][1]);
    assertEquals(resultValue, ySamples[0][1], refb * EPS);

    DoubleMatrix resultValuesMatrix2DSingle = interp.interpolate(xValues, yValues, xSamplesMatrix);
    for (int i = 0; i < nSamples; ++i) {
      for (int k = 0; k < xDim; ++k) {
        final double ref = ySamplesMatrix[0][k][i] == 0. ? 1. : Math.abs(ySamplesMatrix[0][k][i]);
        assertEquals(resultValuesMatrix2DSingle.get(k, i), ySamplesMatrix[0][k][i], ref * EPS);
      }
    }

  }

  /**
   * All of the recovery tests for normal values with Not-A-Knot endpoint conditions
   */
  public void NotAKnotRecoverTest() {

    final double[] xValues = new double[] {1, 3, 2, 4 };
    final double[] yValues = new double[] {-1, 1, 0, 8 };
    final double[][] yValuesMatrix = new double[][] { {-1, 1, 0, 8 }, {20, 0, 5, 5 } };

    final double[] xSamples = new double[] {0, 5. / 2., 7. / 3., 29. / 7. };
    final double[][] ySamples = new double[][] { {-8, 1. / 8., 1. / 27., Math.pow(15. / 7., 3.) }, {45., 5. / 4., 20. / 9., 320. / 49. } };

    final double[][] xSamplesMatrix = new double[][] { {-2, 1, 2, 2.5 }, {4, 5, 6, 7 } };
    final double[][][] ySamplesMatrix = new double[][][] { { {-64., -1., 0., 1. / 8. }, {8., 27., 64., 125. } }, { {125., 20., 5., 5. / 4. }, {5., 20., 45., 80. } } };
    final int xDim = 2;

    final DoubleMatrix coefsExpectedMatrix = DoubleMatrix.copyOf(
        new double[][] {
            {1., -3., 3., -1},
            {0., 5., -20., 20},
            {1., 0., 0., 0.},
            {0., 5., -10., 5},
            {1., 3., 3., 1.},
            {0., 5., 0., 0.}});
    final int dimMatrix = 2;
    final int orderMatrix = 4;
    final int nIntervalsMatrix = 3;
    final double[] knotsMatrix = new double[] {1, 2, 3, 4 };

    CubicSplineInterpolator interpMatrix = new CubicSplineInterpolator();

    PiecewisePolynomialResult resultMatrix = interpMatrix.interpolate(xValues, yValuesMatrix);

    final int nRows = coefsExpectedMatrix.rowCount();
    final int nCols = coefsExpectedMatrix.columnCount();
    for (int i = 0; i < nRows; ++i) {
      for (int j = 0; j < nCols; ++j) {
        final double ref = coefsExpectedMatrix.get(i, j) == 0. ? 1. : Math.abs(coefsExpectedMatrix.get(i, j));
        assertEquals(resultMatrix.getCoefMatrix().get(i, j), coefsExpectedMatrix.get(i, j), ref * EPS);
      }
    }

    assertEquals(resultMatrix.getNumberOfIntervals(), nIntervalsMatrix);
    assertEquals(resultMatrix.getOrder(), orderMatrix);
    assertEquals(resultMatrix.getDimensions(), dimMatrix);
    assertEquals(resultMatrix.getKnots().toArray(), knotsMatrix);

    DoubleMatrix resultValuesMatrix2D = interpMatrix.interpolate(xValues, yValuesMatrix, xSamples);
    final int nSamples = xSamples.length;
    for (int i = 0; i < dimMatrix; ++i) {
      for (int j = 0; j < nSamples; ++j) {
        final double ref = ySamples[i][j] == 0. ? 1. : Math.abs(ySamples[i][j]);
        assertEquals(resultValuesMatrix2D.get(i, j), ySamples[i][j], ref * EPS);
      }
    }

    DoubleArray resultValuesMatrix1D = interpMatrix.interpolate(xValues, yValuesMatrix, xSamples[0]);
    for (int i = 0; i < dimMatrix; ++i) {
      final double ref = ySamples[i][0] == 0. ? 1. : Math.abs(ySamples[i][0]);
      assertEquals(resultValuesMatrix1D.get(i), ySamples[i][0], ref * EPS);
    }

    DoubleMatrix[] resultValuesMatrix2DVec = interpMatrix.interpolate(xValues, yValuesMatrix, xSamplesMatrix);
    for (int i = 0; i < nSamples; ++i) {
      for (int j = 0; j < dimMatrix; ++j) {
        for (int k = 0; k < xDim; ++k) {
          final double ref = ySamplesMatrix[j][k][i] == 0. ? 1. : Math.abs(ySamplesMatrix[j][k][i]);
          assertEquals(resultValuesMatrix2DVec[i].get(j, k), ySamplesMatrix[j][k][i], ref * EPS);
        }
      }
    }

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    for (int i = 0; i < nIntervalsMatrix; ++i) {
      for (int j = 0; j < nCols; ++j) {
        final double ref = coefsExpectedMatrix.get(2 * i, j) == 0. ? 1. : Math.abs(coefsExpectedMatrix.get(2 * i, j));
        assertEquals(result.getCoefMatrix().get(i, j), coefsExpectedMatrix.get(2 * i, j), ref * EPS);
      }
    }

    assertEquals(result.getNumberOfIntervals(), nIntervalsMatrix);
    assertEquals(result.getOrder(), orderMatrix);
    assertEquals(result.getDimensions(), 1);
    assertEquals(result.getKnots().toArray(), knotsMatrix);

    DoubleArray resultValues1D = interp.interpolate(xValues, yValues, xSamples);
    for (int j = 0; j < nSamples; ++j) {
      final double ref = ySamples[0][j] == 0. ? 1. : Math.abs(ySamples[0][j]);
      assertEquals(resultValues1D.get(j), ySamples[0][j], ref * EPS);
    }

    double resultValue = interp.interpolate(xValues, yValues, xSamples[1]);
    final double refb = ySamples[0][1] == 0. ? 1. : Math.abs(ySamples[0][1]);
    assertEquals(resultValue, ySamples[0][1], refb * EPS);

    DoubleMatrix resultValuesMatrix2DSingle = interp.interpolate(xValues, yValues, xSamplesMatrix);
    for (int i = 0; i < nSamples; ++i) {
      for (int k = 0; k < xDim; ++k) {
        final double ref = ySamplesMatrix[0][k][i] == 0. ? 1. : Math.abs(ySamplesMatrix[0][k][i]);
        assertEquals(resultValuesMatrix2DSingle.get(k, i), ySamplesMatrix[0][k][i], ref * EPS);
      }
    }

  }

  /**
   * For a small number of DataPoints with Not-A-Knot endpoint conditions, spline may reduce into linear or quadratic
   * Knots and coefficient Matrix are also reduced in these cases
   */
  public void LinearAndQuadraticNakTest() {
    final double[] xValuesForLin = new double[] {1., 2. };
    final double[][] yValuesForLin = new double[][] { {3., 7. }, {2, -6 } };

    final double[] xValuesForQuad = new double[] {1., 2., 3. };
    final double[][] yValuesForQuad = new double[][] { {1., 6., 5. }, {2., -2., -3. } };

    final double[][] coefsExpectedForLin = new double[][] { {4., 3., }, {-8., 2. } };
    final double[][] coefsExpectedForQuad = new double[][] { {-3., 8., 1. }, {3. / 2., -11. / 2., 2. } };

    final double[][] xKeys = new double[][] { {-0.5, 6. / 5., 2.38 }, {1., 2., 3. } };
    final double[][][] yExpectedForLin = new double[][][] { { {-3., 3.8, 8.52 }, {3., 7., 11. } }, { {14., 0.4, -9.04 }, {2., -6., -14. } } };
    final double[][][] yExpectedForQuad = new double[][][] { { {-17.75, 2.48, 6.3268 }, {1., 6., 5. } }, { {13.625, 0.96, -2.7334 }, {2., -2., -3. } } };

    final int keyDim = xKeys.length;
    final int keyLength = xKeys[0].length;
    final int yDim = yValuesForLin.length;

    /**
     * Linear Interpolation
     */

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    PiecewisePolynomialResult resultLin = interp.interpolate(xValuesForLin, yValuesForLin);

    int nRowsLin = coefsExpectedForLin.length;
    int nColsLin = coefsExpectedForLin[0].length;
    for (int i = 0; i < nRowsLin; ++i) {
      for (int j = 0; j < nColsLin; ++j) {
        final double ref = coefsExpectedForLin[i][j] == 0. ? 1. : Math.abs(coefsExpectedForLin[i][j]);
        assertEquals(resultLin.getCoefMatrix().get(i, j), coefsExpectedForLin[i][j], ref * EPS);
      }
    }

    assertEquals(resultLin.getDimensions(), yDim);
    assertEquals(resultLin.getNumberOfIntervals(), 1);
    assertEquals(resultLin.getKnots().toArray(), xValuesForLin);
    assertEquals(resultLin.getOrder(), 2);

    resultLin = interp.interpolate(xValuesForLin, yValuesForLin[0]);

    for (int i = 0; i < 2; ++i) {
      final double ref = coefsExpectedForLin[0][i] == 0. ? 1. : Math.abs(coefsExpectedForLin[0][i]);
      assertEquals(resultLin.getCoefMatrix().get(0, i), coefsExpectedForLin[0][i], ref * EPS);
    }

    assertEquals(resultLin.getDimensions(), 1);
    assertEquals(resultLin.getNumberOfIntervals(), 1);
    assertEquals(resultLin.getKnots().toArray(), xValuesForLin);
    assertEquals(resultLin.getOrder(), 2);

    DoubleMatrix resultMatrixLin2D = interp.interpolate(xValuesForLin, yValuesForLin[0], xKeys);
    for (int i = 0; i < keyDim; ++i) {
      for (int j = 0; j < keyLength; ++j) {
        final double ref = yExpectedForLin[0][i][j] == 0. ? 1. : Math.abs(yExpectedForLin[0][i][j]);
        assertEquals(resultMatrixLin2D.get(i, j), yExpectedForLin[0][i][j], ref * EPS);
      }
    }

    DoubleArray resultMatrixLin1D = interp.interpolate(xValuesForLin, yValuesForLin[0], xKeys[0]);
    for (int j = 0; j < keyLength; ++j) {
      final double ref = yExpectedForLin[0][0][j] == 0. ? 1. : Math.abs(yExpectedForLin[0][0][j]);
      assertEquals(resultMatrixLin1D.get(j), yExpectedForLin[0][0][j], ref * EPS);
    }

    double resultMatrixLinValue = interp.interpolate(xValuesForLin, yValuesForLin[0], xKeys[0][0]);
    {
      final double ref = yExpectedForLin[0][0][0] == 0. ? 1. : Math.abs(yExpectedForLin[0][0][0]);
      assertEquals(resultMatrixLinValue, yExpectedForLin[0][0][0], ref * EPS);
    }

    DoubleArray resultMatrixLinValues1D = interp.interpolate(xValuesForLin, yValuesForLin, xKeys[0][0]);
    for (int i = 0; i < yDim; ++i) {
      final double ref = yExpectedForLin[i][0][0] == 0. ? 1. : Math.abs(yExpectedForLin[i][0][0]);
      assertEquals(resultMatrixLinValues1D.get(i), yExpectedForLin[i][0][0], ref * EPS);
    }

    DoubleMatrix[] resultMatrixLin2DVec = interp.interpolate(xValuesForLin, yValuesForLin, xKeys);
    for (int i = 0; i < yDim; ++i) {
      for (int j = 0; j < keyDim; ++j) {
        for (int k = 0; k < keyLength; ++k) {
          final double ref = yExpectedForLin[i][j][k] == 0. ? 1. : Math.abs(yExpectedForLin[i][j][k]);
          assertEquals(resultMatrixLin2DVec[k].get(i, j), yExpectedForLin[i][j][k], ref * EPS);
        }
      }
    }

    /**
     * Quadratic Interpolation
     */

    PiecewisePolynomialResult resultQuad = interp.interpolate(xValuesForQuad, yValuesForQuad);

    int nRowsQuad = coefsExpectedForQuad.length;
    int nColsQuad = coefsExpectedForQuad[0].length;
    for (int i = 0; i < nRowsQuad; ++i) {
      for (int j = 0; j < nColsQuad; ++j) {
        final double ref = coefsExpectedForQuad[i][j] == 0. ? 1. : Math.abs(coefsExpectedForQuad[i][j]);
        assertEquals(resultQuad.getCoefMatrix().get(i, j), coefsExpectedForQuad[i][j], ref * EPS);
      }
    }

    assertEquals(resultQuad.getDimensions(), yDim);
    assertEquals(resultQuad.getNumberOfIntervals(), 1);
    assertEquals(resultQuad.getKnots().toArray(), new double[] {xValuesForQuad[0], xValuesForQuad[2]});
    assertEquals(resultQuad.getOrder(), 3);

    resultQuad = interp.interpolate(xValuesForQuad, yValuesForQuad[0]);

    for (int i = 0; i < 3; ++i) {
      final double ref = coefsExpectedForQuad[0][i] == 0. ? 1. : Math.abs(coefsExpectedForQuad[0][i]);
      assertEquals(resultQuad.getCoefMatrix().get(0, i), coefsExpectedForQuad[0][i], ref * EPS);
    }

    assertEquals(resultQuad.getDimensions(), 1);
    assertEquals(resultQuad.getNumberOfIntervals(), 1);
    assertEquals(resultQuad.getKnots().toArray(), new double[] {xValuesForQuad[0], xValuesForQuad[2]});
    assertEquals(resultQuad.getOrder(), 3);

    DoubleMatrix resultMatrixQuad2D = interp.interpolate(xValuesForQuad, yValuesForQuad[0], xKeys);
    for (int i = 0; i < keyDim; ++i) {
      for (int j = 0; j < keyLength; ++j) {
        final double ref = yExpectedForQuad[0][i][j] == 0. ? 1. : Math.abs(yExpectedForQuad[0][i][j]);
        assertEquals(resultMatrixQuad2D.get(i, j), yExpectedForQuad[0][i][j], ref * EPS);
      }
    }

    DoubleArray resultMatrixQuad1D = interp.interpolate(xValuesForQuad, yValuesForQuad[0], xKeys[0]);
    for (int j = 0; j < keyLength; ++j) {
      final double ref = yExpectedForQuad[0][0][j] == 0. ? 1. : Math.abs(yExpectedForQuad[0][0][j]);
      assertEquals(resultMatrixQuad1D.get(j), yExpectedForQuad[0][0][j], ref * EPS);
    }

    double resultMatrixQuadValue = interp.interpolate(xValuesForQuad, yValuesForQuad[0], xKeys[0][0]);
    {
      final double ref = yExpectedForQuad[0][0][0] == 0. ? 1. : Math.abs(yExpectedForQuad[0][0][0]);
      assertEquals(resultMatrixQuadValue, yExpectedForQuad[0][0][0], ref * EPS);
    }

    DoubleArray resultMatrixQuadValues1D = interp.interpolate(xValuesForQuad, yValuesForQuad, xKeys[0][0]);
    for (int i = 0; i < yDim; ++i) {
      final double ref = yExpectedForQuad[i][0][0] == 0. ? 1. : Math.abs(yExpectedForQuad[i][0][0]);
      assertEquals(resultMatrixQuadValues1D.get(i), yExpectedForQuad[i][0][0], ref * EPS);
    }

    DoubleMatrix[] resultMatrixQuad2DVec = interp.interpolate(xValuesForQuad, yValuesForQuad, xKeys);
    for (int i = 0; i < yDim; ++i) {
      for (int j = 0; j < keyDim; ++j) {
        for (int k = 0; k < keyLength; ++k) {
          final double ref = yExpectedForQuad[i][j][k] == 0. ? 1. : Math.abs(yExpectedForQuad[i][j][k]);
          assertEquals(resultMatrixQuad2DVec[k].get(i, j), yExpectedForQuad[i][j][k], ref * EPS);
        }
      }
    }

  }

  /**
   * Number of data should be larger than 1
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void DataShortNakTest() {
    final double[] xValues = new double[] {1. };
    final double[] yValues = new double[] {4. };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void DataShortNakMultiTest() {
    final double[] xValues = new double[] {1. };
    final double[][] yValues = new double[][] { {4. }, {3. } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void DataShortClapmedTest() {
    final double[] xValues = new double[] {1. };
    final double[] yValues = new double[] {0., 4., 3. };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void DataShortClapmedMultiTest() {
    final double[] xValues = new double[] {1. };
    final double[][] yValues = new double[][] { {0., 4., 3. }, {9., 4., 1.5 } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * (yValues length) == (xValues length) + 2 or (yValues length) == (xValues length) should be satisfied
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void WrongDataLengthTest() {
    final double[] xValues = new double[] {1, 2, 3 };
    final double[] yValues = new double[] {2, 3, 4, 5 };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void WrongDataLengthMultiTest() {
    final double[] xValues = new double[] {1, 2, 3 };
    final double[][] yValues = new double[][] { {1, 3, 5, 2 }, {5, 3, 2, 7 }, {1, 8, -1, 0 } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * Repeated data are not allowed
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void RepeatDataTest() {
    final double[] xValues = new double[] {1., 2., 0.5, 8., 1. / 2. };
    final double[] yValues = new double[] {2., 3., 4., 5., 8. };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void RepeatDataMultiTest() {
    final double[] xValues = new double[] {1., 2., 0.5, 8., 1. / 2. };
    final double[][] yValues = new double[][] { {2., 3., 4., 5., 8. }, {2., 1., 4., 2., 8. } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * Data are null
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullTest() {
    final double[] xValues = null;
    final double[] yValues = null;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullmultiTest() {
    final double[] xValues = null;
    final double[][] yValues = null;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * Data are infinite-valued
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfinityXTest() {

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    final double zero = 0.;

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = 1. / zero;
      yValues[i] = i;
    }

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfinityYTest() {

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    final double zero = 0.;

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = i;
      yValues[i] = 1. / zero;
    }

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfinityXMultiTest() {

    final int nPts = 5;
    final int nDim = 3;
    double[] xValues = new double[nPts];
    double[][] yValues = new double[nDim][nPts];

    final double zero = 0.;

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = i;
      for (int j = 0; j < nDim; ++j) {
        yValues[j][i] = i;
      }
    }
    xValues[1] = 1. / zero;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfinityYMultiTest() {

    final int nPts = 5;
    final int nDim = 3;
    double[] xValues = new double[nPts];
    double[][] yValues = new double[nDim][nPts];

    final double zero = 0.;

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = i;
      for (int j = 0; j < nDim; ++j) {
        yValues[j][i] = i;
      }
    }
    yValues[1][2] = 1. / zero;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);
  }

  /**
   * Data are NaN
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNXTest() {

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = i;
      yValues[i] = i;
    }

    xValues[1] = Double.NaN;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNYTest() {

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = i;
      yValues[i] = i;
    }

    yValues[1] = Double.NaN;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNXMultiTest() {

    final int nPts = 5;
    final int nDim = 3;
    double[] xValues = new double[nPts];
    double[][] yValues = new double[nDim][nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = i;
      for (int j = 0; j < nDim; ++j) {
        yValues[j][i] = i;
      }
    }

    xValues[1] = Double.NaN;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNYMultiTest() {

    final int nPts = 5;
    final int nDim = 3;
    double[] xValues = new double[nPts];
    double[][] yValues = new double[nDim][nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = i;
      for (int j = 0; j < nDim; ++j) {
        yValues[j][i] = i;
      }
    }

    yValues[1][0] = Double.NaN;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNOutputNakTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[] yValues = new double[] {1., 6.e307, -2.e306, 3. };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNOutputClampedTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[] yValues = new double[] {2., 1., 6.e307, -2.e306, 3., 6. };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNOutputNakMultiTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {1., 2., 3., 4. }, {1., 6.e307, -2.e306, 3. } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNOutputClampedMultiTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {3., 1., 2., 3., 4., 1. }, {100., 1., 6.e307, -2.e306, 3., 2 } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * Infinite output due to large data
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfOutputNakTest() {

    final double[] xValues = new double[] {1., 1.000001 };
    final double[] yValues = new double[] {1., 3.e307 };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfOutputClampedTest() {

    final double[] xValues = new double[] {1., 1.000001 };
    final double[] yValues = new double[] {0., 1., 3.e307, 0. };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfOutputNakMultiTest() {

    final double[] xValues = new double[] {1., 1.000001 };
    final double[][] yValues = new double[][] { {1., 2. }, {1., 3.e307 } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfOutputClampedMultiTest() {

    final double[] xValues = new double[] {1., 1.000001 };
    final double[][] yValues = new double[][] { {3., 1., 2., 1. }, {0., 1., 3.e307, 0. } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfOutputNakQuadTest() {

    final double[] xValues = new double[] {1., 1.000001, 1.000002 };
    final double[] yValues = new double[] {1., 3.e307, 3.e-307 };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfOutputNakQuadMultiTest() {

    final double[] xValues = new double[] {1., 1.000001, 1.000002 };
    final double[][] yValues = new double[][] { {2., 3., 4. }, {1., 3.e307, 3.e-307 } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues);

  }

  /**
   * Infinite output due to large key
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeKeyTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[] yValues = new double[] {8., 6., 7., 8. };
    final double key = 3.e103;

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues, key);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeMultiKeyTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[] yValues = new double[] {8., 6., 7., 8. };
    final double[] key = new double[] {1., 3., 3.e103 };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();
    interp.interpolate(xValues, yValues, key);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeKeyMultiTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {8., 6., 7., 8. }, {3., 12., 1., 8. } };
    final double key = 3.e103;
    CubicSplineInterpolator interp = new CubicSplineInterpolator();
    interp.interpolate(xValues, yValues, key);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeMultiKeyMultiTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {8., 6., 7., 8. }, {3., 12., 1., 8. } };
    final double[] key = new double[] {1., 3., 3.e103 };
    CubicSplineInterpolator interp = new CubicSplineInterpolator();
    interp.interpolate(xValues, yValues, key);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeMatrixKeyTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[] yValues = new double[] {8., 6., 7., 8. };
    final double[][] key = new double[][] { {1., 3., 3.e103 }, {0.1, 2., 5. } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues, key);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeMatrixKeyMultiTest() {

    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {8., 6., 7., 8. }, {3., 12., 1., 8. } };
    final double[][] key = new double[][] { {1., 3., 3.e103 }, {0.1, 2., 5. } };

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues, key);

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

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

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

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

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

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

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

    CubicSplineInterpolator interp = new CubicSplineInterpolator();

    interp.interpolate(xValues, yValues, xKey);

  }

}
