/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

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
  public PiecewisePolynomialResult interpolate(double[] xValues, double[] yValues) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "(xValues length = yValues length) should be true");
    ArgChecker.isTrue(xValues.length > 2, "Data points should be >= 3");

    int nDataPts = xValues.length;

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

    double[] intervals = _solver.intervalsCalculator(xValuesSrt);
    double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
    double[] first = firstDerivativeCalculator(slopes);
    double[][] coefs = _solver.solve(yValuesSrt, intervals, slopes, first);

    for (int i = 0; i < nDataPts - 1; ++i) {
      double ref = 0.;
      for (int j = 0; j < 4; ++j) {
        ref += coefs[i][j] * Math.pow(intervals[i], 3 - j);
        ArgChecker.isFalse(Double.isNaN(coefs[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefs[i][j]), "Too large input");
      }
      double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValuesSrt[i + 1]) < ERROR * bound,
          "Input is too large/small or data points are too close");
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(coefs), 4, 1);
  }

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[][] yValuesMatrix) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValuesMatrix, "yValuesMatrix");

    ArgChecker.isTrue(xValues.length == yValuesMatrix[0].length,
        "(xValues length = yValuesMatrix's row vector length) should be true");
    ArgChecker.isTrue(xValues.length > 2, "Data points should be >= 3");

    int nDataPts = xValues.length;
    int yValuesLen = yValuesMatrix[0].length;
    int dim = yValuesMatrix.length;

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
    DoubleMatrix[] coefMatrix = new DoubleMatrix[dim];

    for (int i = 0; i < dim; ++i) {
      xValuesSrt = Arrays.copyOf(xValues, nDataPts);
      double[] yValuesSrt = Arrays.copyOf(yValuesMatrix[i], nDataPts);
      DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

      double[] intervals = _solver.intervalsCalculator(xValuesSrt);
      double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
      double[] first = firstDerivativeCalculator(slopes);

      coefMatrix[i] = DoubleMatrix.copyOf(_solver.solve(yValuesSrt, intervals, slopes, first));

      for (int k = 0; k < intervals.length; ++k) {
        double ref = 0.;
        for (int j = 0; j < 4; ++j) {
          ref += coefMatrix[i].get(k, j) * Math.pow(intervals[k], 3 - j);
          ArgChecker.isFalse(Double.isNaN(coefMatrix[i].get(k, j)), "Too large input");
          ArgChecker.isFalse(Double.isInfinite(coefMatrix[i].get(k, j)), "Too large input");
        }
        double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[k + 1]), 1.e-1);
        ArgChecker.isTrue(Math.abs(ref - yValuesSrt[k + 1]) < ERROR * bound,
            "Input is too large/small or data points are too close");
      }
    }

    int nIntervals = coefMatrix[0].rowCount();
    int nCoefs = coefMatrix[0].columnCount();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].row(i).toArray();
      }
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(resMatrix), nCoefs, dim);
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(double[] xValues, double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "(xValues length = yValues length) should be true");
    ArgChecker.isTrue(xValues.length > 2, "Data points should be >= 3");

    int nDataPts = xValues.length;

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

    double[] intervals = _solver.intervalsCalculator(xValues);
    double[] slopes = _solver.slopesCalculator(yValues, intervals);
    double[][] slopeSensitivity = _solver.slopeSensitivityCalculator(intervals);
    DoubleArray[] firstWithSensitivity =
        firstDerivativeWithSensitivityCalculator(yValues, intervals, slopes, slopeSensitivity);
    DoubleMatrix[] resMatrix = _solver.solveWithSensitivity(yValues, intervals, slopes, slopeSensitivity, firstWithSensitivity);

    for (int k = 0; k < nDataPts; k++) {
      DoubleMatrix m = resMatrix[k];
      int rows = m.rowCount();
      int cols = m.columnCount();
      for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
          ArgChecker.isTrue(Doubles.isFinite(m.get(i, j)), "Matrix contains a NaN or infinite");
        }
      }
    }

    DoubleMatrix coefMatrix = resMatrix[0];
    for (int i = 0; i < nDataPts - 1; ++i) {
      double ref = 0.;
      for (int j = 0; j < 4; ++j) {
        ref += coefMatrix.get(i, j) * Math.pow(intervals[i], 3 - j);
      }
      double bound = Math.max(Math.abs(ref) + Math.abs(yValues[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValues[i + 1]) < ERROR * bound,
          "Input is too large/small or data points are too close");
    }
    DoubleMatrix[] coefSenseMatrix = new DoubleMatrix[nDataPts - 1];
    System.arraycopy(resMatrix, 1, coefSenseMatrix, 0, nDataPts - 1);
    int nCoefs = coefMatrix.columnCount();

    return new PiecewisePolynomialResultsWithSensitivity(
        DoubleArray.copyOf(xValues), coefMatrix, nCoefs, 1, coefSenseMatrix);
  }

  private double[] firstDerivativeCalculator(double[] slopes) {
    int nData = slopes.length + 1;
    double[] res = new double[nData];

    double[] slopesExt = getExtraPoints(slopes);
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

  private DoubleArray[] firstDerivativeWithSensitivityCalculator(
      double[] yValues,
      double[] intervals,
      double[] slopes,
      double[][] slopeSensitivity) {

    int nData = yValues.length;
    double[] slopesExt = getExtraPoints(slopes);
    double[][] slopeSensitivityExtTransp = new double[nData][nData + 3];
    DoubleArray[] res = new DoubleArray[nData + 1];
    DoubleMatrix senseMat = DoubleMatrix.copyOf(slopeSensitivity);

    for (int i = 0; i < nData; ++i) {
      slopeSensitivityExtTransp[i] = getExtraPoints(senseMat.column(i).toArray());
    }

    DoubleArray[] modSlopesWithSensitivity = modSlopesWithSensitivityCalculator(slopesExt, slopeSensitivityExtTransp);
    DoubleArray modSlopesWithSense0 = modSlopesWithSensitivity[0];

    double[] first = new double[nData];
    for (int i = 0; i < nData; ++i) {
      double[] tmp = new double[nData];
      double den = (modSlopesWithSense0.get(i + 2) + modSlopesWithSense0.get(i));
      if (den == 0.) {
        first[i] = 0.5 * (slopesExt[i + 1] + slopesExt[i + 2]);

        Arrays.fill(tmp, 0.);
        double[] yValuesUp = Arrays.copyOf(yValues, nData);
        double[] yValuesDw = Arrays.copyOf(yValues, nData);
        for (int j = 0; j < nData; ++j) {
          double div = Math.abs(yValues[j]) < SMALL ? EPS : yValues[j] * EPS;
          yValuesUp[j] = Math.abs(yValues[j]) < SMALL ? EPS : yValues[j] * (1. + EPS);
          yValuesDw[j] = Math.abs(yValues[j]) < SMALL ? -EPS : yValues[j] * (1. - EPS);
          double firstUp = firstDerivativeCalculator(_solver.slopesCalculator(yValuesUp, intervals))[i];
          double firstDw = firstDerivativeCalculator(_solver.slopesCalculator(yValuesDw, intervals))[i];
          tmp[j] = 0.5 * (firstUp - firstDw) / div;
          yValuesUp[j] = yValues[j];
          yValuesDw[j] = yValues[j];
        }
      } else {
        first[i] =
            modSlopesWithSense0.get(i + 2) * slopesExt[i + 1] / den +
                modSlopesWithSense0.get(i) * slopesExt[i + 2] / den;
        for (int k = 0; k < nData; ++k) {
          tmp[k] =
              (modSlopesWithSense0.get(i + 2) * slopeSensitivityExtTransp[k][i + 1] +
                  modSlopesWithSense0.get(i) * slopeSensitivityExtTransp[k][i + 2]) / den
              + (slopesExt[i + 2] - slopesExt[i + 1]) *
                  (modSlopesWithSense0.get(i + 2) * modSlopesWithSensitivity[i + 1].get(k) -
                  modSlopesWithSense0.get(i) * modSlopesWithSensitivity[i + 3].get(k)) / den / den;
        }
      }
      res[i + 1] = DoubleArray.copyOf(tmp);
    }
    res[0] = DoubleArray.copyOf(first);

    return res;
  }

  private DoubleArray[] modSlopesWithSensitivityCalculator(double[] slopesExt, double[][] slopeSensitivityExtTransp) {
    int nData = slopesExt.length - 3;
    double[] modSlopes = new double[nData + 2];
    DoubleArray[] res = new DoubleArray[nData + 3];

    for (int i = 0; i < nData + 2; ++i) {
      double[] tmp = new double[nData];
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
      res[i + 1] = DoubleArray.copyOf(tmp);
    }
    res[0] = DoubleArray.copyOf(modSlopes);

    return res;
  }

  private double[] getExtraPoints(double[] data) {
    int nData = data.length + 1;
    double[] res = new double[nData + 3];
    res[0] = 3. * data[0] - 2. * data[1];
    res[1] = 2. * data[0] - data[1];
    res[nData + 1] = 2. * data[nData - 2] - data[nData - 3];
    res[nData + 2] = 3 * data[nData - 2] - 2. * data[nData - 3];
    System.arraycopy(data, 0, res, 2, nData - 1);

    return res;
  }

}
