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
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;

/**
 * Filter for local monotonicity of cubic spline interpolation based on 
 * R. L. Dougherty, A. Edelman, and J. M. Hyman, "Nonnegativity-, Monotonicity-, or Convexity-Preserving Cubic and Quintic Hermite Interpolation" 
 * Mathematics Of Computation, v. 52, n. 186, April 1989, pp. 471-494. 
 * 
 * First, interpolant is computed by another cubic interpolation method. Then the first derivatives are modified such that local monotonicity conditions are satisfied. 
 */
public class MonotonicityPreservingCubicSplineInterpolator extends PiecewisePolynomialInterpolator {
  private final HermiteCoefficientsProvider _solver = new HermiteCoefficientsProvider();
  private final PiecewisePolynomialWithSensitivityFunction1D _function = new PiecewisePolynomialWithSensitivityFunction1D();
  private PiecewisePolynomialInterpolator _method;

  private static final double EPS = 1.e-7;
  private static final double SMALL = 1.e-14;

  /**
   * Primary interpolation method should be passed
   * @param method PiecewisePolynomialInterpolator
   */
  public MonotonicityPreservingCubicSplineInterpolator(final PiecewisePolynomialInterpolator method) {
    _method = method;
  }

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length | xValues.length + 2 == yValues.length, "(xValues length = yValues length) or (xValues length + 2 = yValues length)");
    ArgChecker.isTrue(xValues.length > 4, "Data points should be more than 4");

    final int nDataPts = xValues.length;
    final int yValuesLen = yValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
    }
    for (int i = 0; i < yValuesLen; ++i) {
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yValues containing Infinity");
    }

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = new double[nDataPts];
    if (nDataPts == yValuesLen) {
      yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    } else {
      yValuesSrt = Arrays.copyOfRange(yValues, 1, nDataPts + 1);
    }
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

    for (int i = 1; i < nDataPts; ++i) {
      ArgChecker.isFalse(xValuesSrt[i - 1] == xValuesSrt[i], "xValues should be distinct");
    }

    final double[] intervals = _solver.intervalsCalculator(xValuesSrt);
    final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
    final PiecewisePolynomialResult result = _method.interpolate(xValues, yValues);

    ArgChecker.isTrue(result.getOrder() == 4, "Primary interpolant is not cubic");

    final double[] initialFirst = _function.differentiate(result, xValuesSrt).rowArray(0);
    final double[] first = firstDerivativeCalculator(intervals, slopes, initialFirst);
    final double[][] coefs = _solver.solve(yValuesSrt, intervals, slopes, first);

    for (int i = 0; i < nDataPts - 1; ++i) {
      for (int j = 0; j < 4; ++j) {
        ArgChecker.isFalse(Double.isNaN(coefs[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefs[i][j]), "Too large input");
      }
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(coefs), 4, 1);
  }

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[][] yValuesMatrix) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValuesMatrix, "yValuesMatrix");

    ArgChecker.isTrue(xValues.length == yValuesMatrix[0].length | xValues.length + 2 == yValuesMatrix[0].length,
        "(xValues length = yValuesMatrix's row vector length) or (xValues length + 2 = yValuesMatrix's row vector length)");
    ArgChecker.isTrue(xValues.length > 4, "Data points should be more than 4");

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
    DoubleMatrix[] coefMatrix = new DoubleMatrix[dim];

    for (int i = 0; i < dim; ++i) {
      xValuesSrt = Arrays.copyOf(xValues, nDataPts);
      double[] yValuesSrt = new double[nDataPts];
      if (nDataPts == yValuesLen) {
        yValuesSrt = Arrays.copyOf(yValuesMatrix[i], nDataPts);
      } else {
        yValuesSrt = Arrays.copyOfRange(yValuesMatrix[i], 1, nDataPts + 1);
      }
      DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

      final double[] intervals = _solver.intervalsCalculator(xValuesSrt);
      final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
      final PiecewisePolynomialResult result = _method.interpolate(xValues, yValuesMatrix[i]);

      ArgChecker.isTrue(result.getOrder() == 4, "Primary interpolant is not cubic");

      final double[] initialFirst = _function.differentiate(result, xValuesSrt).rowArray(0);
      final double[] first = firstDerivativeCalculator(intervals, slopes, initialFirst);

      coefMatrix[i] = DoubleMatrix.copyOf(_solver.solve(yValuesSrt, intervals, slopes, first));
    }

    final int nIntervals = coefMatrix[0].rowCount();
    final int nCoefs = coefMatrix[0].columnCount();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].row(i).toArray();
      }
    }

    for (int i = 0; i < (nIntervals * dim); ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        ArgChecker.isFalse(Double.isNaN(resMatrix[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(resMatrix[i][j]), "Too large input");
      }
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(resMatrix), nCoefs, dim);
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(final double[] xValues, final double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length | xValues.length + 2 == yValues.length, "(xValues length = yValues length) or (xValues length + 2 = yValues length)");
    ArgChecker.isTrue(xValues.length > 4, "Data points should be more than 4");

    final int nDataPts = xValues.length;
    final int yValuesLen = yValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
    }
    for (int i = 0; i < yValuesLen; ++i) {
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yValues containing Infinity");
    }

    double[] yValuesSrt = new double[nDataPts];
    if (nDataPts == yValuesLen) {
      yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    } else {
      yValuesSrt = Arrays.copyOfRange(yValues, 1, nDataPts + 1);
    }

    for (int i = 1; i < nDataPts; ++i) {
      ArgChecker.isFalse(xValues[i - 1] == xValues[i], "xValues should be distinct");
    }

    final double[] intervals = _solver.intervalsCalculator(xValues);
    final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
    final DoubleMatrix[] slopesSensitivityWithAbs = slopesSensitivityWithAbsCalculator(intervals, slopes);
    final double[][] slopesSensitivity = slopesSensitivityWithAbs[0].toArray();
    final double[][] slopesAbsSensitivity = slopesSensitivityWithAbs[1].toArray();
    DoubleArray[] firstWithSensitivity = new DoubleArray[nDataPts + 1];

    /*
     * Mode sensitivity is not computed analytically for |s_i| = |s_{i+1}| or s_{i-1}*h_{i} + s_{i}*h_{i-1} = 0. 
     * Centered finite difference approximation is used in such cases
     */
    final boolean sym = checkSymm(slopes);
    if (sym == true) {
      final PiecewisePolynomialResult result = _method.interpolate(xValues, yValues);
      ArgChecker.isTrue(result.getOrder() == 4, "Primary interpolant is not cubic");
      final double[] initialFirst = _function.differentiate(result, xValues).rowArray(0);
      firstWithSensitivity[0] = DoubleArray.copyOf(firstDerivativeCalculator(intervals, slopes, initialFirst));

      int nExtra = nDataPts == yValuesLen ? 0 : 1;
      final double[] yValuesUp = Arrays.copyOf(yValues, nDataPts + 2 * nExtra);
      final double[] yValuesDw = Arrays.copyOf(yValues, nDataPts + 2 * nExtra);
      final double[][] tmp = new double[nDataPts][nDataPts];
      for (int i = nExtra; i < nDataPts + nExtra; ++i) {
        final double den = Math.abs(yValues[i]) < SMALL ? EPS : yValues[i] * EPS;
        yValuesUp[i] = Math.abs(yValues[i]) < SMALL ? EPS : yValues[i] * (1. + EPS);
        yValuesDw[i] = Math.abs(yValues[i]) < SMALL ? -EPS : yValues[i] * (1. - EPS);
        final double[] yValuesSrtUp = Arrays.copyOfRange(yValuesUp, nExtra, nDataPts + nExtra);
        final double[] yValuesSrtDw = Arrays.copyOfRange(yValuesDw, nExtra, nDataPts + nExtra);

        final double[] slopesUp = _solver.slopesCalculator(yValuesSrtUp, intervals);
        final double[] slopesDw = _solver.slopesCalculator(yValuesSrtDw, intervals);
        final double[] initialFirstUp = _function.differentiate(_method.interpolate(xValues, yValuesUp), xValues).rowArray(0);
        final double[] initialFirstDw = _function.differentiate(_method.interpolate(xValues, yValuesDw), xValues).rowArray(0);
        final double[] firstUp = firstDerivativeCalculator(intervals, slopesUp, initialFirstUp);
        final double[] firstDw = firstDerivativeCalculator(intervals, slopesDw, initialFirstDw);
        for (int j = 0; j < nDataPts; ++j) {
          tmp[j][i - nExtra] = 0.5 * (firstUp[j] - firstDw[j]) / den;
        }
        yValuesUp[i] = yValues[i];
        yValuesDw[i] = yValues[i];
      }
      for (int i = 0; i < nDataPts; ++i) {
        firstWithSensitivity[i + 1] = DoubleArray.copyOf(tmp[i]);
      }
    } else {
      final PiecewisePolynomialResultsWithSensitivity resultWithSensitivity = _method.interpolateWithSensitivity(xValues, yValues);
      ArgChecker.isTrue(resultWithSensitivity.getOrder() == 4, "Primary interpolant is not cubic");

      final double[] initialFirst = _function.differentiate(resultWithSensitivity, xValues).rowArray(0);
      final DoubleArray[] initialFirstSense = _function.differentiateNodeSensitivity(resultWithSensitivity, xValues);
      firstWithSensitivity = firstDerivativeWithSensitivityCalculator(intervals, slopes, slopesSensitivity, slopesAbsSensitivity, initialFirst, initialFirstSense);
    }
    final DoubleMatrix[] resMatrix = _solver.solveWithSensitivity(yValuesSrt, intervals, slopes, slopesSensitivity, firstWithSensitivity);

    for (int k = 0; k < nDataPts; k++) {
      DoubleMatrix m = resMatrix[k];
      final int rows = m.rowCount();
      final int cols = m.columnCount();
      for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
          ArgChecker.isTrue(Doubles.isFinite(m.get(i, j)), "Matrix contains a NaN or infinite");
        }
      }
    }

    final DoubleMatrix coefMatrix = resMatrix[0];
    final DoubleMatrix[] coefSenseMatrix = new DoubleMatrix[nDataPts - 1];
    System.arraycopy(resMatrix, 1, coefSenseMatrix, 0, nDataPts - 1);
    final int nCoefs = coefMatrix.columnCount();

    return new PiecewisePolynomialResultsWithSensitivity(DoubleArray.copyOf(xValues), coefMatrix, nCoefs, 1, coefSenseMatrix);
  }

  @Override
  public PiecewisePolynomialInterpolator getPrimaryMethod() {
    return _method;
  }

  /**
   * First derivatives are modified such that cubic interpolant has the same sign as linear interpolator 
   * @param yValues 
   * @param intervals 
   * @param slopes 
   * @param initialFirst 
   * @return first derivative 
   */
  private double[] firstDerivativeCalculator(final double[] intervals, final double[] slopes, final double[] initialFirst) {
    final int nDataPts = intervals.length + 1;
    double[] res = new double[nDataPts];
    final double[][] pSlopes = parabolaSlopesCalculator(intervals, slopes);

    for (int i = 1; i < nDataPts - 1; ++i) {
      double refValue = 3. * Math.min(Math.abs(slopes[i - 1]), Math.min(Math.abs(slopes[i]), Math.abs(pSlopes[i - 1][1])));
      if (i > 1) {
        final double sig1 = Math.signum(pSlopes[i - 1][1]);
        final double sig2 = Math.signum(pSlopes[i - 1][0]);
        final double sig3 = Math.signum(slopes[i - 1] - slopes[i - 2]);
        final double sig4 = Math.signum(slopes[i] - slopes[i - 1]);
        if (Math.abs(sig1 - sig2) <= 0. && Math.abs(sig2 - sig3) <= 0. && Math.abs(sig3 - sig4) <= 0.) {
          refValue = Math.max(refValue, 1.5 * Math.min(Math.abs(pSlopes[i - 1][0]), Math.abs(pSlopes[i - 1][1])));
        }
      }
      if (i < nDataPts - 2) {
        final double sig1 = Math.signum(-pSlopes[i - 1][1]);
        final double sig2 = Math.signum(-pSlopes[i - 1][2]);
        final double sig3 = Math.signum(slopes[i + 1] - slopes[i]);
        final double sig4 = Math.signum(slopes[i] - slopes[i - 1]);
        if (Math.abs(sig1 - sig2) <= 0. && Math.abs(sig2 - sig3) <= 0. && Math.abs(sig3 - sig4) <= 0.) {
          refValue = Math.max(refValue, 1.5 * Math.min(Math.abs(pSlopes[i - 1][2]), Math.abs(pSlopes[i - 1][1])));
        }
      }
      res[i] = Math.signum(initialFirst[i]) != Math.signum(pSlopes[i - 1][1]) ? 0. : Math.signum(initialFirst[i]) * Math.min(Math.abs(initialFirst[i]), refValue);
    }
    res[0] = Math.signum(initialFirst[0]) != Math.signum(slopes[0]) ? 0. : Math.signum(initialFirst[0]) * Math.min(Math.abs(initialFirst[0]), 3. * Math.abs(slopes[0]));
    res[nDataPts - 1] = Math.signum(initialFirst[nDataPts - 1]) != Math.signum(slopes[nDataPts - 2]) ? 0. : Math.signum(initialFirst[nDataPts - 1])
        * Math.min(Math.abs(initialFirst[nDataPts - 1]), 3. * Math.abs(slopes[nDataPts - 2]));
    return res;
  }

  private DoubleArray[] firstDerivativeWithSensitivityCalculator(final double[] intervals, final double[] slopes, final double[][] slopesSensitivity,
      final double[][] slopesAbsSensitivity, final double[] initialFirst, final DoubleArray[] initialFirstSense) {
    final int nDataPts = intervals.length + 1;
    final DoubleArray[] res = new DoubleArray[nDataPts + 1];
    final double[] first = new double[nDataPts];
    final double[][] pSlopes = parabolaSlopesCalculator(intervals, slopes);
    final DoubleMatrix[] pSlopesAbsSensitivity = parabolaSlopesAbstSensitivityCalculator(intervals, slopesSensitivity, pSlopes);

    for (int i = 1; i < nDataPts - 1; ++i) {
      final double[] tmpSense = new double[nDataPts];
      final double sigInitialFirst = Math.signum(initialFirst[i]);
      if (sigInitialFirst * Math.signum(pSlopes[i - 1][1]) < 0.) {
        first[i] = 0.;
        Arrays.fill(tmpSense, 0.);
      } else {
        double[] refValueWithSense = factoredMinWithSensitivityFinder(
            Math.abs(slopes[i - 1]),
            slopesAbsSensitivity[i - 1],
            Math.abs(slopes[i]),
            slopesAbsSensitivity[i],
            Math.abs(pSlopes[i - 1][1]),
            pSlopesAbsSensitivity[1].rowArray(i - 1));
        final double[] refSense = new double[nDataPts];
        System.arraycopy(refValueWithSense, 1, refSense, 0, nDataPts);
        if (i > 1) {
          final double sig1 = Math.signum(pSlopes[i - 1][1]);
          final double sig2 = Math.signum(pSlopes[i - 1][0]);
          final double sig3 = Math.signum(slopes[i - 1] - slopes[i - 2]);
          final double sig4 = Math.signum(slopes[i] - slopes[i - 1]);
          if (Math.abs(sig1 - sig2) <= 0. && Math.abs(sig2 - sig3) <= 0. && Math.abs(sig3 - sig4) <= 0.) {
            refValueWithSense = modifyRefValueWithSensitivity(
                refValueWithSense[0],
                refSense,
                Math.abs(pSlopes[i - 1][0]),
                pSlopesAbsSensitivity[0].rowArray(i - 2),
                Math.abs(pSlopes[i - 1][1]),
                pSlopesAbsSensitivity[1].rowArray(i - 1));
          }
        }
        if (i < nDataPts - 2) {
          final double sig1 = Math.signum(-pSlopes[i - 1][1]);
          final double sig2 = Math.signum(-pSlopes[i - 1][2]);
          final double sig3 = Math.signum(slopes[i + 1] - slopes[i]);
          final double sig4 = Math.signum(slopes[i] - slopes[i - 1]);
          if (Math.abs(sig1 - sig2) <= 0. && Math.abs(sig2 - sig3) <= 0. && Math.abs(sig3 - sig4) <= 0.) {
            refValueWithSense = modifyRefValueWithSensitivity(
                refValueWithSense[0],
                refSense,
                Math.abs(pSlopes[i - 1][2]),
                pSlopesAbsSensitivity[2].rowArray(i - 1),
                Math.abs(pSlopes[i - 1][1]),
                pSlopesAbsSensitivity[1].rowArray(i - 1));
          }
        }
        final double absFirst = Math.abs(initialFirst[i]);
        if (Math.abs(absFirst - refValueWithSense[0]) < SMALL) {
          first[i] = absFirst <= refValueWithSense[0] ? initialFirst[i] : sigInitialFirst * refValueWithSense[0];
          for (int k = 0; k < nDataPts; ++k) {
            tmpSense[k] = 0.5 * (initialFirstSense[i].get(k) + sigInitialFirst * refValueWithSense[k + 1]);
          }
        } else {
          if (absFirst < refValueWithSense[0]) {
            first[i] = initialFirst[i];
            System.arraycopy(initialFirstSense[i].toArray(), 0, tmpSense, 0, nDataPts);
          } else {
            first[i] = sigInitialFirst * refValueWithSense[0];
            for (int k = 0; k < nDataPts; ++k) {
              tmpSense[k] = sigInitialFirst * refValueWithSense[k + 1];
            }
          }
        }
      }
      res[i + 1] = DoubleArray.copyOf(tmpSense);
    }

    final double[] tmpSenseIni = new double[nDataPts];
    final double sigFirstIni = Math.signum(initialFirst[0]);
    if (sigFirstIni * Math.signum(slopes[0]) < 0.) {
      first[0] = 0.;
      Arrays.fill(tmpSenseIni, 0.);
    } else {
      if (Math.abs(initialFirst[0]) > SMALL && Math.abs(slopes[0]) < SMALL) {
        first[0] = 0.;
        Arrays.fill(tmpSenseIni, 0.);
        tmpSenseIni[0] = -1.5 / intervals[0];
        tmpSenseIni[1] = 1.5 / intervals[0];
      } else {
        final double absFirst = Math.abs(initialFirst[0]);
        final double modSlope = 3. * Math.abs(slopes[0]);
        if (Math.abs(absFirst - modSlope) < SMALL) {
          first[0] = absFirst <= modSlope ? initialFirst[0] : sigFirstIni * modSlope;
          for (int k = 0; k < nDataPts; ++k) {
            tmpSenseIni[k] = 0.5 * (initialFirstSense[0].get(k) + 3. * sigFirstIni * slopesAbsSensitivity[0][k]);
          }
        } else {
          if (absFirst < modSlope) {
            first[0] = initialFirst[0];
            final double factor = Math.abs(initialFirst[0]) < SMALL ? 0.5 : 1.;
            for (int k = 0; k < nDataPts; ++k) {
              tmpSenseIni[k] = factor * initialFirstSense[0].get(k);
            }
          } else {
            first[0] = sigFirstIni * modSlope;
            for (int k = 0; k < nDataPts; ++k) {
              tmpSenseIni[k] = 3. * sigFirstIni * slopesAbsSensitivity[0][k];
            }
          }
        }
      }
    }
    res[1] = DoubleArray.copyOf(tmpSenseIni);

    final double[] tmpSenseFin = new double[nDataPts];
    final double sigFirstFin = Math.signum(initialFirst[nDataPts - 1]);
    if (sigFirstFin * Math.signum(slopes[nDataPts - 2]) < 0.) {
      first[nDataPts - 1] = 0.;
      Arrays.fill(tmpSenseFin, 0.);
    } else {
      if (Math.abs(initialFirst[nDataPts - 1]) > SMALL && Math.abs(slopes[nDataPts - 2]) < SMALL) {
        first[nDataPts - 1] = 0.;
        Arrays.fill(tmpSenseFin, 0.);
        tmpSenseFin[nDataPts - 2] = -1.5 / intervals[nDataPts - 2];
        tmpSenseFin[nDataPts - 1] = 1.5 / intervals[nDataPts - 2];
      } else {
        final double absFirst = Math.abs(initialFirst[nDataPts - 1]);
        final double modSlope = 3. * Math.abs(slopes[nDataPts - 2]);
        if (Math.abs(absFirst - modSlope) < SMALL) {
          first[nDataPts - 1] = absFirst <= modSlope ? initialFirst[nDataPts - 1] : sigFirstFin * modSlope;
          for (int k = 0; k < nDataPts; ++k) {
            tmpSenseFin[k] =
                0.5 * (initialFirstSense[nDataPts - 1].get(k) + 3. * sigFirstFin * slopesAbsSensitivity[nDataPts - 2][k]);
          }
        } else {
          if (absFirst < modSlope) {
            first[nDataPts - 1] = initialFirst[nDataPts - 1];
            final double factor = Math.abs(initialFirst[nDataPts - 1]) < SMALL ? 0.5 : 1.;
            for (int k = 0; k < nDataPts; ++k) {
              tmpSenseFin[k] = factor * initialFirstSense[nDataPts - 1].get(k);
            }
          } else {
            first[nDataPts - 1] = sigFirstFin * modSlope;
            for (int k = 0; k < nDataPts; ++k) {
              tmpSenseFin[k] = 3. * sigFirstFin * slopesAbsSensitivity[nDataPts - 2][k];
            }
          }
        }
      }
    }
    res[nDataPts] = DoubleArray.copyOf(tmpSenseFin);

    res[0] = DoubleArray.copyOf(first);
    return res;
  }

  /**
   * @param intervals 
   * @param slopes 
   * @return Parabola slopes, each row vactor is (p^{-1}, p^{0}, p^{1}) for xValues_1,...,xValues_{nDataPts-2}
   */
  private double[][] parabolaSlopesCalculator(final double[] intervals, final double[] slopes) {
    final int nData = intervals.length + 1;
    double[][] res = new double[nData - 2][3];

    res[0][0] = Double.POSITIVE_INFINITY;
    res[0][1] = (slopes[0] * intervals[1] + slopes[1] * intervals[0]) / (intervals[0] + intervals[1]);
    res[0][2] = (slopes[1] * (2. * intervals[1] + intervals[2]) - slopes[2] * intervals[1]) / (intervals[1] + intervals[2]);
    for (int i = 1; i < nData - 3; ++i) {
      res[i][0] = (slopes[i] * (2. * intervals[i] + intervals[i - 1]) - slopes[i - 1] * intervals[i]) / (intervals[i - 1] + intervals[i]);
      res[i][1] = (slopes[i] * intervals[i + 1] + slopes[i + 1] * intervals[i]) / (intervals[i] + intervals[i + 1]);
      res[i][2] = (slopes[i + 1] * (2. * intervals[i + 1] + intervals[i + 2]) - slopes[i + 2] * intervals[i + 1]) / (intervals[i + 1] + intervals[i + 2]);
    }
    res[nData - 3][0] = (slopes[nData - 3] * (2. * intervals[nData - 3] + intervals[nData - 4]) - slopes[nData - 4] * intervals[nData - 3]) / (intervals[nData - 4] + intervals[nData - 3]);
    res[nData - 3][1] = (slopes[nData - 3] * intervals[nData - 2] + slopes[nData - 2] * intervals[nData - 3]) / (intervals[nData - 3] + intervals[nData - 2]);
    res[nData - 3][2] = Double.POSITIVE_INFINITY;
    return res;
  }

  private DoubleMatrix[] parabolaSlopesAbstSensitivityCalculator(final double[] intervals, final double[][] slopeSensitivity, final double[][] parabolaSlopes) {
    final DoubleMatrix[] res = new DoubleMatrix[3];
    final int nData = intervals.length + 1;

    final double[][] left = new double[nData - 3][nData];
    final double[][] center = new double[nData - 2][nData];
    final double[][] right = new double[nData - 3][nData];
    for (int i = 0; i < nData - 3; ++i) {
      final double sigLeft = Math.signum(parabolaSlopes[i + 1][0]);
      final double sigCenter = Math.signum(parabolaSlopes[i][1]);
      final double sigRight = Math.signum(parabolaSlopes[i][2]);
      if (sigLeft == 0.) {
        Arrays.fill(left[i], 0.);
      }
      if (sigCenter == 0.) {
        Arrays.fill(center[i], 0.);
      }
      if (sigRight == 0.) {
        Arrays.fill(right[i], 0.);
      }
      for (int k = 0; k < nData; ++k) {
        left[i][k] = sigLeft * (slopeSensitivity[i + 1][k] * (2. * intervals[i + 1] + intervals[i]) - slopeSensitivity[i][k] * intervals[i + 1]) /
            (intervals[i] + intervals[i + 1]);
        center[i][k] = sigCenter * (slopeSensitivity[i][k] * intervals[i + 1] + slopeSensitivity[i + 1][k] * intervals[i]) / (intervals[i] + intervals[i + 1]);
        right[i][k] = sigRight * (slopeSensitivity[i + 1][k] * (2. * intervals[i + 1] + intervals[i + 2]) - slopeSensitivity[i + 2][k] * intervals[i + 1]) /
            (intervals[i + 1] + intervals[i + 2]);
      }
    }
    final double sigCenterFin = Math.signum(parabolaSlopes[nData - 3][1]);
    if (sigCenterFin == 0.) {
      Arrays.fill(center[nData - 3], 0.);
    }
    for (int k = 0; k < nData; ++k) {
      center[nData - 3][k] = sigCenterFin * (slopeSensitivity[nData - 3][k] * intervals[nData - 2] + slopeSensitivity[nData - 2][k] * intervals[nData - 3]) /
          (intervals[nData - 3] + intervals[nData - 2]);
    }
    res[0] = DoubleMatrix.copyOf(left);
    res[1] = DoubleMatrix.copyOf(center);
    res[2] = DoubleMatrix.copyOf(right);

    return res;
  }

  private DoubleMatrix[] slopesSensitivityWithAbsCalculator(final double[] intervals, final double[] slopes) {
    final int nDataPts = intervals.length + 1;
    final DoubleMatrix[] res = new DoubleMatrix[2];
    final double[][] slopesSensitivity = new double[nDataPts - 1][nDataPts];
    final double[][] absSlopesSensitivity = new double[nDataPts - 1][nDataPts];

    for (int i = 0; i < nDataPts - 1; ++i) {
      final double sign = Math.signum(slopes[i]);
      Arrays.fill(slopesSensitivity[i], 0.);
      Arrays.fill(absSlopesSensitivity[i], 0.);
      slopesSensitivity[i][i] = -1. / intervals[i];
      slopesSensitivity[i][i + 1] = 1. / intervals[i];
      if (sign > 0.) {
        absSlopesSensitivity[i][i] = slopesSensitivity[i][i];
        absSlopesSensitivity[i][i + 1] = slopesSensitivity[i][i + 1];
      }
      if (sign < 0.) {
        absSlopesSensitivity[i][i] = -slopesSensitivity[i][i];
        absSlopesSensitivity[i][i + 1] = -slopesSensitivity[i][i + 1];
      }
    }
    res[0] = DoubleMatrix.copyOf(slopesSensitivity);
    res[1] = DoubleMatrix.copyOf(absSlopesSensitivity);
    return res;
  }

  private double[] factoredMinWithSensitivityFinder(final double val1, final double[] val1Sensitivity, final double val2, final double[] val2Sensitivity, final double val3,
      final double[] val3Sensitivity) {
    final int nData = val1Sensitivity.length;
    final double[] res = new double[nData + 1];
    double tmpRef = 0.;
    final double[] tmpSensitivity = new double[nData];

    if (val1 < val2) {
      tmpRef = val1;
      for (int i = 0; i < nData; ++i) {
        tmpSensitivity[i] = val1Sensitivity[i];
      }
    } else {
      tmpRef = val2;
      for (int i = 0; i < nData; ++i) {
        tmpSensitivity[i] = val2Sensitivity[i];
      }
    }

    if (val3 == tmpRef) {
      res[0] = 3. * val3;
      for (int i = 0; i < nData; ++i) {
        res[i + 1] = 1.5 * (val3Sensitivity[i] + tmpSensitivity[i]);
      }
    } else {
      if (val3 < tmpRef) {
        res[0] = 3. * val3;
        for (int i = 0; i < nData; ++i) {
          res[i + 1] = 3. * val3Sensitivity[i];
        }
      } else {
        res[0] = 3. * tmpRef;
        for (int i = 0; i < nData; ++i) {
          res[i + 1] = 3. * tmpSensitivity[i];
        }
      }
    }

    return res;
  }

  private double[] modifyRefValueWithSensitivity(final double refVal, final double[] refValSensitivity, final double val1, final double[] val1Sensitivity, final double val2,
      final double[] val2Sensitivity) {
    final int nData = refValSensitivity.length;
    final double absVal1 = Math.abs(val1);
    final double absVal2 = Math.abs(val2);
    final double[] res = new double[nData + 1];
    double tmpRef = 0.;
    final double[] tmpSensitivity = new double[nData];

    if (absVal1 == absVal2) {
      tmpRef = 1.5 * absVal1;
      for (int i = 0; i < nData; ++i) {
        tmpSensitivity[i] = 0.75 * (val1Sensitivity[i] + val2Sensitivity[i]);
      }
    } else {
      if (absVal1 < absVal2) {
        tmpRef = 1.5 * absVal1;
        for (int i = 0; i < nData; ++i) {
          tmpSensitivity[i] = 1.5 * (val1Sensitivity[i]);
        }
      } else {
        tmpRef = 1.5 * absVal2;
        for (int i = 0; i < nData; ++i) {
          tmpSensitivity[i] = 1.5 * (val2Sensitivity[i]);
        }
      }
    }

    if (refVal == tmpRef) {
      res[0] = refVal;
      for (int i = 0; i < nData; ++i) {
        res[i + 1] = 0.5 * (refValSensitivity[i] + tmpSensitivity[i]);
      }
    } else {
      if (refVal > tmpRef) {
        res[0] = refVal;
        for (int i = 0; i < nData; ++i) {
          res[i + 1] = refValSensitivity[i];
        }
      } else {
        res[0] = tmpRef;
        for (int i = 0; i < nData; ++i) {
          res[i + 1] = tmpSensitivity[i];
        }
      }
    }

    return res;
  }

  private boolean checkSymm(final double[] slopes) {
    final int nDataM2 = slopes.length - 1;
    for (int i = 0; i < nDataM2; ++i) {
      if (Math.abs(Math.abs(slopes[i]) - Math.abs(slopes[i + 1])) < SMALL) {
        return true;
      }
    }
    return false;
  }
}
