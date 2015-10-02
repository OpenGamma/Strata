/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Cubic spline interpolation based on 
 * H. Akima, "A New Method of Interpolation and Smooth Curve Fitting Based on Local Procedures," 
 * Journal of the Association for Computing Machinery, Vol 17, no 4, October 1970, 589-602
 */
public class SemiLocalCubicSplineInterpolator extends PiecewisePolynomialInterpolator {
  private static final double ERROR = 1.e-13;
  private static final double EPS = 1.e-7;
  private static final double SMALL = 1.e-14;
  private final HermiteCoefficientsProvider _solver = new HermiteCoefficientsProvider();

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[] yValues) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "(xValues length = yValues length) should be true");
    ArgChecker.isTrue(xValues.length > 2, "Data points should be >= 3");

    final int nDataPts = xValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yValues containing Infinity");
    }

    for (int i = 0; i < nDataPts - 1; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

    final double[] intervals = _solver.intervalsCalculator(xValuesSrt);
    final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
    final double[] first = firstDerivativeCalculator(slopes);
    final double[][] coefs = _solver.solve(yValuesSrt, intervals, slopes, first);

    for (int i = 0; i < nDataPts - 1; ++i) {
      double ref = 0.;
      for (int j = 0; j < 4; ++j) {
        ref += coefs[i][j] * Math.pow(intervals[i], 3 - j);
        ArgChecker.isFalse(Double.isNaN(coefs[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefs[i][j]), "Too large input");
      }
      final double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValuesSrt[i + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
    }

    return new PiecewisePolynomialResult(new DoubleMatrix1D(xValuesSrt), new DoubleMatrix2D(coefs), 4, 1);
  }

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[][] yValuesMatrix) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValuesMatrix, "yValuesMatrix");

    ArgChecker.isTrue(xValues.length == yValuesMatrix[0].length, "(xValues length = yValuesMatrix's row vector length) should be true");
    ArgChecker.isTrue(xValues.length > 2, "Data points should be >= 3");

    final int nDataPts = xValues.length;
    final int yValuesLen = yValuesMatrix[0].length;
    final int dim = yValuesMatrix.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
    }
    for (int i = 0; i < yValuesLen; ++i) {
      for (int j = 0; j < dim; ++j) {
        ArgChecker.isFalse(Double.isNaN(yValuesMatrix[j][i]), "yValuesMatrix containing NaN");
        ArgChecker.isFalse(Double.isInfinite(yValuesMatrix[j][i]), "yValuesMatrix containing Infinity");
      }
    }
    for (int i = 0; i < nDataPts; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] xValuesSrt = new double[nDataPts];
    DoubleMatrix2D[] coefMatrix = new DoubleMatrix2D[dim];

    for (int i = 0; i < dim; ++i) {
      xValuesSrt = Arrays.copyOf(xValues, nDataPts);
      double[] yValuesSrt = Arrays.copyOf(yValuesMatrix[i], nDataPts);
      DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

      final double[] intervals = _solver.intervalsCalculator(xValuesSrt);
      final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
      final double[] first = firstDerivativeCalculator(slopes);

      coefMatrix[i] = new DoubleMatrix2D(_solver.solve(yValuesSrt, intervals, slopes, first));

      for (int k = 0; k < intervals.length; ++k) {
        double ref = 0.;
        for (int j = 0; j < 4; ++j) {
          ref += coefMatrix[i].getData()[k][j] * Math.pow(intervals[k], 3 - j);
          ArgChecker.isFalse(Double.isNaN(coefMatrix[i].getData()[k][j]), "Too large input");
          ArgChecker.isFalse(Double.isInfinite(coefMatrix[i].getData()[k][j]), "Too large input");
        }
        final double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[k + 1]), 1.e-1);
        ArgChecker.isTrue(Math.abs(ref - yValuesSrt[k + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
      }
    }

    final int nIntervals = coefMatrix[0].getNumberOfRows();
    final int nCoefs = coefMatrix[0].getNumberOfColumns();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].getRowVector(i).getData();
      }
    }

    return new PiecewisePolynomialResult(new DoubleMatrix1D(xValuesSrt), new DoubleMatrix2D(resMatrix), nCoefs, dim);
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(final double[] xValues, final double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "(xValues length = yValues length) should be true");
    ArgChecker.isTrue(xValues.length > 2, "Data points should be >= 3");

    final int nDataPts = xValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yValues containing Infinity");
    }

    for (int i = 0; i < nDataPts - 1; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    final double[] intervals = _solver.intervalsCalculator(xValues);
    final double[] slopes = _solver.slopesCalculator(yValues, intervals);
    final double[][] slopeSensitivity = _solver.slopeSensitivityCalculator(intervals);
    final DoubleMatrix1D[] firstWithSensitivity = firstDerivativeWithSensitivityCalculator(yValues, intervals, slopes, slopeSensitivity);
    final DoubleMatrix2D[] resMatrix = _solver.solveWithSensitivity(yValues, intervals, slopes, slopeSensitivity, firstWithSensitivity);

    for (int k = 0; k < nDataPts; k++) {
      DoubleMatrix2D m = resMatrix[k];
      final int rows = m.getNumberOfRows();
      final int cols = m.getNumberOfColumns();
      for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
          ArgChecker.isTrue(Doubles.isFinite(m.getEntry(i, j)), "Matrix contains a NaN or infinite");
        }
      }
    }

    final DoubleMatrix2D coefMatrix = resMatrix[0];
    for (int i = 0; i < nDataPts - 1; ++i) {
      double ref = 0.;
      for (int j = 0; j < 4; ++j) {
        ref += coefMatrix.getData()[i][j] * Math.pow(intervals[i], 3 - j);
      }
      final double bound = Math.max(Math.abs(ref) + Math.abs(yValues[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValues[i + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
    }
    final DoubleMatrix2D[] coefSenseMatrix = new DoubleMatrix2D[nDataPts - 1];
    System.arraycopy(resMatrix, 1, coefSenseMatrix, 0, nDataPts - 1);
    final int nCoefs = coefMatrix.getNumberOfColumns();

    return new PiecewisePolynomialResultsWithSensitivity(new DoubleMatrix1D(xValues), coefMatrix, nCoefs, 1, coefSenseMatrix);
  }

  private double[] firstDerivativeCalculator(final double[] slopes) {
    final int nData = slopes.length + 1;
    double[] res = new double[nData];

    final double[] slopesExt = getExtraPoints(slopes);
    for (int i = 0; i < nData; ++i) {
      if (Math.abs(slopesExt[i + 3] - slopesExt[i + 2]) == 0.) {
        if (Math.abs(slopesExt[i + 1] - slopesExt[i]) == 0.) {
          res[i] = 0.5 * (slopesExt[i + 1] + slopesExt[i + 2]);
        } else {
          res[i] = slopesExt[i + 2];
        }
      } else {
        if (Math.abs(slopesExt[i + 1] - slopesExt[i]) == 0.) {
          res[i] = slopesExt[i];
        } else {
          res[i] = (Math.abs(slopesExt[i + 3] - slopesExt[i + 2]) * slopesExt[i + 1] + Math.abs(slopesExt[i + 1] - slopesExt[i]) * slopesExt[i + 2]) /
              (Math.abs(slopesExt[i + 3] - slopesExt[i + 2]) + Math.abs(slopesExt[i + 1] - slopesExt[i]));
        }
      }
    }

    return res;
  }

  private DoubleMatrix1D[] firstDerivativeWithSensitivityCalculator(final double[] yValues, final double[] intervals, final double[] slopes, final double[][] slopeSensitivity) {
    final int nData = yValues.length;
    final double[] slopesExt = getExtraPoints(slopes);
    final double[][] slopeSensitivityExtTransp = new double[nData][nData + 3];
    final DoubleMatrix1D[] res = new DoubleMatrix1D[nData + 1];
    DoubleMatrix2D senseMat = new DoubleMatrix2D(slopeSensitivity);

    for (int i = 0; i < nData; ++i) {
      slopeSensitivityExtTransp[i] = getExtraPoints(senseMat.getColumnVector(i).getData());
    }

    final DoubleMatrix1D[] modSlopesWithSensitivity = modSlopesWithSensitivityCalculator(slopesExt, slopeSensitivityExtTransp);

    final double[] first = new double[nData];
    for (int i = 0; i < nData; ++i) {
      final double[] tmp = new double[nData];
      final double den = (modSlopesWithSensitivity[0].getData()[i + 2] + modSlopesWithSensitivity[0].getData()[i]);
      if (den == 0.) {
        first[i] = 0.5 * (slopesExt[i + 1] + slopesExt[i + 2]);

        Arrays.fill(tmp, 0.);
        double[] yValuesUp = Arrays.copyOf(yValues, nData);
        double[] yValuesDw = Arrays.copyOf(yValues, nData);
        for (int j = 0; j < nData; ++j) {
          final double div = Math.abs(yValues[j]) < SMALL ? EPS : yValues[j] * EPS;
          yValuesUp[j] = Math.abs(yValues[j]) < SMALL ? EPS : yValues[j] * (1. + EPS);
          yValuesDw[j] = Math.abs(yValues[j]) < SMALL ? -EPS : yValues[j] * (1. - EPS);
          final double firstUp = firstDerivativeCalculator(_solver.slopesCalculator(yValuesUp, intervals))[i];
          final double firstDw = firstDerivativeCalculator(_solver.slopesCalculator(yValuesDw, intervals))[i];
          tmp[j] = 0.5 * (firstUp - firstDw) / div;
          yValuesUp[j] = yValues[j];
          yValuesDw[j] = yValues[j];
        }
      } else {
        first[i] = modSlopesWithSensitivity[0].getData()[i + 2] * slopesExt[i + 1] / den + modSlopesWithSensitivity[0].getData()[i] * slopesExt[i + 2] / den;
        for (int k = 0; k < nData; ++k) {
          tmp[k] = (modSlopesWithSensitivity[0].getData()[i + 2] * slopeSensitivityExtTransp[k][i + 1] + modSlopesWithSensitivity[0].getData()[i] * slopeSensitivityExtTransp[k][i + 2]) / den
              + (slopesExt[i + 2] - slopesExt[i + 1]) *
              (modSlopesWithSensitivity[0].getData()[i + 2] * modSlopesWithSensitivity[i + 1].getData()[k] - modSlopesWithSensitivity[0].getData()[i] * modSlopesWithSensitivity[i + 3].getData()[k]) /
              den / den;
        }
      }
      res[i + 1] = new DoubleMatrix1D(tmp);
    }
    res[0] = new DoubleMatrix1D(first);

    return res;
  }

  private DoubleMatrix1D[] modSlopesWithSensitivityCalculator(final double[] slopesExt, final double[][] slopeSensitivityExtTransp) {
    final int nData = slopesExt.length - 3;
    final double[] modSlopes = new double[nData + 2];
    final DoubleMatrix1D[] res = new DoubleMatrix1D[nData + 3];

    for (int i = 0; i < nData + 2; ++i) {
      final double[] tmp = new double[nData];
      if (slopesExt[i + 1] == slopesExt[i]) {
        modSlopes[i] = 0.;
        Arrays.fill(tmp, 0.);
      } else {
        if (slopesExt[i + 1] > slopesExt[i]) {
          modSlopes[i] = slopesExt[i + 1] - slopesExt[i];
          for (int k = 0; k < nData; ++k) {
            tmp[k] = slopeSensitivityExtTransp[k][i + 1] - slopeSensitivityExtTransp[k][i];
          }
        } else {
          modSlopes[i] = -slopesExt[i + 1] + slopesExt[i];
          for (int k = 0; k < nData; ++k) {
            tmp[k] = -slopeSensitivityExtTransp[k][i + 1] + slopeSensitivityExtTransp[k][i];
          }
        }
      }
      res[i + 1] = new DoubleMatrix1D(tmp);
    }
    res[0] = new DoubleMatrix1D(modSlopes);

    return res;
  }

  private double[] getExtraPoints(final double[] data) {
    final int nData = data.length + 1;
    final double[] res = new double[nData + 3];
    res[0] = 3. * data[0] - 2. * data[1];
    res[1] = 2. * data[0] - data[1];
    res[nData + 1] = 2. * data[nData - 2] - data[nData - 3];
    res[nData + 2] = 3 * data[nData - 2] - 2. * data[nData - 3];
    System.arraycopy(data, 0, res, 2, nData - 1);

    return res;
  }

}
