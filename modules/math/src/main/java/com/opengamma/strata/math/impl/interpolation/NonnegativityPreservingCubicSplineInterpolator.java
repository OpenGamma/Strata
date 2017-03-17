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
 * Filter for nonnegativity of cubic spline interpolation based on 
 * R. L. Dougherty, A. Edelman, and J. M. Hyman, "Nonnegativity-, Monotonicity-, or Convexity-Preserving Cubic and Quintic Hermite Interpolation" 
 * Mathematics Of Computation, v. 52, n. 186, April 1989, pp. 471-494. 
 * 
 * First, interpolant is computed by another cubic interpolation method. Then the first derivatives are modified such that non-negativity conditions are satisfied. 
 * Note that shape-preserving three-point formula is used at endpoints in order to ensure positivity of an interpolant in the first interval and the last interval 
 */
public class NonnegativityPreservingCubicSplineInterpolator extends PiecewisePolynomialInterpolator {

  private static final double SMALL = 1.e-14;

  private final HermiteCoefficientsProvider _solver = new HermiteCoefficientsProvider();
  private final PiecewisePolynomialWithSensitivityFunction1D _function = new PiecewisePolynomialWithSensitivityFunction1D();
  private PiecewisePolynomialInterpolator _method;

  /**
   * Primary interpolation method should be passed.
   * @param method PiecewisePolynomialInterpolator
   */
  public NonnegativityPreservingCubicSplineInterpolator(final PiecewisePolynomialInterpolator method) {
    _method = method;
  }

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length | xValues.length + 2 == yValues.length, "(xValues length = yValues length) or (xValues length + 2 = yValues length)");
    ArgChecker.isTrue(xValues.length > 2, "Data points should be more than 2");

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

    for (int i = 0; i < nDataPts - 1; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = new double[nDataPts];
    if (nDataPts == yValuesLen) {
      yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    } else {
      yValuesSrt = Arrays.copyOfRange(yValues, 1, nDataPts + 1);
    }
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

    final double[] intervals = _solver.intervalsCalculator(xValuesSrt);
    final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
    final PiecewisePolynomialResult result = _method.interpolate(xValues, yValues);

    ArgChecker.isTrue(result.getOrder() == 4, "Primary interpolant is not cubic");

    final double[] initialFirst = _function.differentiate(result, xValuesSrt).rowArray(0);
    final double[] first = firstDerivativeCalculator(yValuesSrt, intervals, slopes, initialFirst);
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
    ArgChecker.isTrue(xValues.length > 2, "Data points should be more than 2");

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
      final double[] first = firstDerivativeCalculator(yValuesSrt, intervals, slopes, initialFirst);

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
    ArgChecker.isTrue(xValues.length > 2, "Data points should be more than 2");

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

    for (int i = 0; i < nDataPts - 1; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] yValuesSrt = new double[nDataPts];
    if (nDataPts == yValuesLen) {
      yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    } else {
      yValuesSrt = Arrays.copyOfRange(yValues, 1, nDataPts + 1);
    }

    final double[] intervals = _solver.intervalsCalculator(xValues);
    final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
    final PiecewisePolynomialResultsWithSensitivity resultWithSensitivity = _method.interpolateWithSensitivity(xValues, yValues);

    ArgChecker.isTrue(resultWithSensitivity.getOrder() == 4, "Primary interpolant is not cubic");

    final double[] initialFirst = _function.differentiate(resultWithSensitivity, xValues).rowArray(0);
    final double[][] slopeSensitivity = _solver.slopeSensitivityCalculator(intervals);
    final DoubleArray[] initialFirstSense = _function.differentiateNodeSensitivity(resultWithSensitivity, xValues);
    final DoubleArray[] firstWithSensitivity = firstDerivativeWithSensitivityCalculator(yValuesSrt, intervals, initialFirst, initialFirstSense);
    final DoubleMatrix[] resMatrix = _solver.solveWithSensitivity(yValuesSrt, intervals, slopes, slopeSensitivity, firstWithSensitivity);

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

  // First derivatives are modified such that cubic interpolant has the same sign as linear interpolator 
  private double[] firstDerivativeCalculator(final double[] yValues, final double[] intervals, final double[] slopes, final double[] initialFirst) {
    final int nDataPts = yValues.length;
    double[] res = new double[nDataPts];

    for (int i = 1; i < nDataPts - 1; ++i) {
      final double tau = Math.signum(yValues[i]);
      res[i] = tau == 0. ? initialFirst[i] : Math.min(3. * tau * yValues[i] / intervals[i - 1], Math.max(-3. * tau * yValues[i] / intervals[i], tau * initialFirst[i])) / tau;
    }
    final double tauIni = Math.signum(yValues[0]);
    final double tauFin = Math.signum(yValues[nDataPts - 1]);
    res[0] = tauIni == 0. ? initialFirst[0] : Math.min(3. * tauIni * yValues[0] / intervals[0], Math.max(-3. * tauIni * yValues[0] / intervals[0], tauIni * initialFirst[0])) / tauIni;
    res[nDataPts - 1] = tauFin == 0. ? initialFirst[nDataPts - 1] : Math.min(3. * tauFin * yValues[nDataPts - 1] / intervals[nDataPts - 2],
        Math.max(-3. * tauFin * yValues[nDataPts - 1] / intervals[nDataPts - 2], tauFin * initialFirst[nDataPts - 1])) /
        tauFin;

    return res;
  }

  private DoubleArray[] firstDerivativeWithSensitivityCalculator(final double[] yValues, final double[] intervals, final double[] initialFirst,
      final DoubleArray[] initialFirstSense) {
    final int nDataPts = yValues.length;
    final DoubleArray[] res = new DoubleArray[nDataPts + 1];
    final double[] newFirst = new double[nDataPts];

    for (int i = 1; i < nDataPts - 1; ++i) {
      final double tau = Math.signum(yValues[i]);
      final double lower = -3. * tau * yValues[i] / intervals[i];
      final double upper = 3. * tau * yValues[i] / intervals[i - 1];
      final double ref = tau * initialFirst[i];
      final double[] tmp = new double[nDataPts];
      Arrays.fill(tmp, 0.);
      if (Math.abs(ref - lower) < SMALL && tau != 0.) {
        newFirst[i] = ref >= lower ? initialFirst[i] : lower / tau;
        for (int k = 0; k < nDataPts; ++k) {
          tmp[k] = 0.5 * initialFirstSense[i].get(k);
        }
        tmp[i] -= 1.5 / intervals[i];
      } else {
        if (ref < lower) {
          newFirst[i] = lower / tau;
          tmp[i] = -3. / intervals[i];
        } else {
          if (Math.abs(ref - upper) < SMALL && tau != 0.) {
            newFirst[i] = ref <= upper ? initialFirst[i] : upper / tau;
            for (int k = 0; k < nDataPts; ++k) {
              tmp[k] = 0.5 * initialFirstSense[i].get(k);
            }
            tmp[i] += 1.5 / intervals[i - 1];
          } else {
            if (ref > upper) {
              newFirst[i] = upper / tau;
              tmp[i] = 3. / intervals[i - 1];
            } else {
              newFirst[i] = initialFirst[i];
              System.arraycopy(initialFirstSense[i].toArray(), 0, tmp, 0, nDataPts);
            }
          }
        }
      }
      res[i + 1] = DoubleArray.copyOf(tmp);
    }
    final double tauIni = Math.signum(yValues[0]);
    final double lowerIni = -3. * tauIni * yValues[0] / intervals[0];
    final double upperIni = 3. * tauIni * yValues[0] / intervals[0];
    final double refIni = tauIni * initialFirst[0];
    final double[] tmpIni = new double[nDataPts];
    Arrays.fill(tmpIni, 0.);
    if (Math.abs(refIni - lowerIni) < SMALL && tauIni != 0.) {
      newFirst[0] = refIni >= lowerIni ? initialFirst[0] : lowerIni / tauIni;
      for (int k = 0; k < nDataPts; ++k) {
        tmpIni[k] = 0.5 * initialFirstSense[0].get(k);
      }
      tmpIni[0] -= 1.5 / intervals[0];
    } else {
      if (refIni < lowerIni) {
        newFirst[0] = lowerIni / tauIni;
        tmpIni[0] = -3. / intervals[0];
      } else {
        if (Math.abs(refIni - upperIni) < SMALL && tauIni != 0.) {
          newFirst[0] = refIni <= upperIni ? initialFirst[0] : upperIni / tauIni;
          for (int k = 0; k < nDataPts; ++k) {
            tmpIni[k] = 0.5 * initialFirstSense[0].get(k);
          }
          tmpIni[0] += 1.5 / intervals[0];
        } else {
          if (refIni > upperIni) {
            newFirst[0] = upperIni / tauIni;
            tmpIni[0] = 3. / intervals[0];
          } else {
            newFirst[0] = initialFirst[0];
            System.arraycopy(initialFirstSense[0].toArray(), 0, tmpIni, 0, nDataPts);
          }
        }
      }
    }
    res[1] = DoubleArray.copyOf(tmpIni);
    final double tauFin = Math.signum(yValues[nDataPts - 1]);
    final double lowerFin = -3. * tauFin * yValues[nDataPts - 1] / intervals[nDataPts - 2];
    final double upperFin = 3. * tauFin * yValues[nDataPts - 1] / intervals[nDataPts - 2];
    final double refFin = tauFin * initialFirst[nDataPts - 1];
    final double[] tmpFin = new double[nDataPts];
    Arrays.fill(tmpFin, 0.);
    if (Math.abs(refFin - lowerFin) < SMALL && tauFin != 0.) {
      newFirst[nDataPts - 1] = refFin >= lowerFin ? initialFirst[nDataPts - 1] : lowerFin / tauFin;
      for (int k = 0; k < nDataPts; ++k) {
        tmpFin[k] = 0.5 * initialFirstSense[nDataPts - 1].get(k);
      }
      tmpFin[nDataPts - 1] -= 1.5 / intervals[nDataPts - 2];
    } else {
      if (refFin < lowerFin) {
        newFirst[nDataPts - 1] = lowerFin / tauFin;
        tmpFin[nDataPts - 1] = -3. / intervals[nDataPts - 2];
      } else {
        if (Math.abs(refFin - upperFin) < SMALL && tauFin != 0.) {
          newFirst[nDataPts - 1] = refFin <= upperFin ? initialFirst[nDataPts - 1] : upperFin / tauFin;
          for (int k = 0; k < nDataPts; ++k) {
            tmpFin[k] = 0.5 * initialFirstSense[nDataPts - 1].get(k);
          }
          tmpFin[nDataPts - 1] += 1.5 / intervals[nDataPts - 2];
        } else {
          if (refFin > upperFin) {
            newFirst[nDataPts - 1] = upperFin / tauFin;
            tmpFin[nDataPts - 1] = 3. / intervals[nDataPts - 2];
          } else {
            newFirst[nDataPts - 1] = initialFirst[nDataPts - 1];
            System.arraycopy(initialFirstSense[nDataPts - 1].toArray(), 0, tmpFin, 0, nDataPts);
          }
        }
      }
    }
    res[nDataPts] = DoubleArray.copyOf(tmpFin);
    res[0] = DoubleArray.copyOf(newFirst);
    return res;
  }

}
