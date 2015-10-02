/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Give a class {@link PiecewisePolynomialResultsWithSensitivity}, Compute node sensitivity of function value, first derivative value and second derivative value
 */
public class PiecewisePolynomialWithSensitivityFunction1D extends PiecewisePolynomialFunction1D {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  /** 
   * @param pp {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKey  the key
   * @return Node sensitivity value at x=xKey
   */
  public DoubleMatrix1D nodeSensitivity(final PiecewisePolynomialResultsWithSensitivity pp, final double xKey) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }

    final double[] knots = pp.getKnots().getData();
    final int nKnots = knots.length;
    int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
    if (interval == nKnots - 1) {
      interval--; // there is 1 less interval that knots
    }

    final double s = xKey - knots[interval];
    final DoubleMatrix2D a = pp.getCoefficientSensitivity(interval);
    final int nCoefs = a.getNumberOfRows();

    DoubleMatrix1D res = a.getRowVector(0);
    for (int i = 1; i < nCoefs; i++) {
      res = (DoubleMatrix1D) MA.scale(res, s);
      res = (DoubleMatrix1D) MA.add(res, a.getRowVector(i));
    }

    return res;
  }

  /** 
   * @param pp {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKeys  the keys
   * @return Node sensitivity value at x=xKeys
   */
  public DoubleMatrix1D[] nodeSensitivity(final PiecewisePolynomialResultsWithSensitivity pp, final double[] xKeys) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.notNull(xKeys, "null xKeys");
    final int nKeys = xKeys.length;
    final DoubleMatrix1D[] res = new DoubleMatrix1D[nKeys];

    for (int i = 0; i < nKeys; ++i) {
      ArgChecker.isFalse(Double.isNaN(xKeys[i]), "xKey containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xKeys[i]), "xKey containing Infinity");
    }
    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }

    final double[] knots = pp.getKnots().getData();
    final int nKnots = knots.length;

    for (int j = 0; j < nKeys; ++j) {
      final double xKey = xKeys[j];
      int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
      if (interval == nKnots - 1) {
        interval--; // there is 1 less interval that knots
      }

      final double s = xKey - knots[interval];
      final DoubleMatrix2D a = pp.getCoefficientSensitivity(interval);
      final int nCoefs = a.getNumberOfRows();

      res[j] = a.getRowVector(0);
      for (int i = 1; i < nCoefs; i++) {
        res[j] = (DoubleMatrix1D) MA.scale(res[j], s);
        res[j] = (DoubleMatrix1D) MA.add(res[j], a.getRowVector(i));
      }
    }

    return res;
  }

  /** 
   * @param pp {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKey  the key
   * @return Node sensitivity of derivative value at x=xKey
   */
  public DoubleMatrix1D differentiateNodeSensitivity(final PiecewisePolynomialResultsWithSensitivity pp, final double xKey) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    final int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 2, "Polynomial degree is too low");

    final double[] knots = pp.getKnots().getData();
    final int nKnots = knots.length;
    int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
    if (interval == nKnots - 1) {
      interval--; // there is 1 less interval that knots
    }

    final double s = xKey - knots[interval];
    final DoubleMatrix2D a = pp.getCoefficientSensitivity(interval);

    DoubleMatrix1D res = (DoubleMatrix1D) MA.scale(a.getRowVector(0), nCoefs - 1);
    for (int i = 1; i < nCoefs - 1; i++) {
      res = (DoubleMatrix1D) MA.scale(res, s);
      res = (DoubleMatrix1D) MA.add(res, MA.scale(a.getRowVector(i), nCoefs - 1 - i));
    }

    return res;
  }

  /** 
   * @param pp {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKeys  the keys
   * @return Node sensitivity of derivative value at x=xKeys
   */
  public DoubleMatrix1D[] differentiateNodeSensitivity(final PiecewisePolynomialResultsWithSensitivity pp, final double[] xKeys) {
    ArgChecker.notNull(pp, "null pp");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    final int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 2, "Polynomial degree is too low");
    final int nIntervals = pp.getNumberOfIntervals();

    final DoubleMatrix2D[] diffSense = new DoubleMatrix2D[nIntervals];
    final DoubleMatrix2D[] senseMat = pp.getCoefficientSensitivityAll();
    final int nData = senseMat[0].getNumberOfColumns();
    for (int i = 0; i < nIntervals; ++i) {
      final double[][] tmp = new double[nCoefs - 1][nData];
      for (int j = 0; j < nCoefs - 1; ++j) {
        for (int k = 0; k < nData; ++k) {
          tmp[j][k] = (nCoefs - 1 - j) * senseMat[i].getData()[j][k];
        }
      }
      diffSense[i] = new DoubleMatrix2D(tmp);
    }

    PiecewisePolynomialResultsWithSensitivity ppDiff = new PiecewisePolynomialResultsWithSensitivity(pp.getKnots(), pp.getCoefMatrix(), nCoefs - 1, pp.getDimensions(), diffSense);
    return nodeSensitivity(ppDiff, xKeys);
  }

  /** 
   * @param pp {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKey  the key
   * @return Node sensitivity of second derivative value at x=xKey
   */
  public DoubleMatrix1D differentiateTwiceNodeSensitivity(final PiecewisePolynomialResultsWithSensitivity pp, final double xKey) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    final int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 3, "Polynomial degree is too low");

    final double[] knots = pp.getKnots().getData();
    final int nKnots = knots.length;
    int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
    if (interval == nKnots - 1) {
      interval--; // there is 1 less interval that knots
    }

    final double s = xKey - knots[interval];
    final DoubleMatrix2D a = pp.getCoefficientSensitivity(interval);

    DoubleMatrix1D res = (DoubleMatrix1D) MA.scale(a.getRowVector(0), (nCoefs - 1) * (nCoefs - 2));
    for (int i = 1; i < nCoefs - 2; i++) {
      res = (DoubleMatrix1D) MA.scale(res, s);
      res = (DoubleMatrix1D) MA.add(res, MA.scale(a.getRowVector(i), (nCoefs - 1 - i) * (nCoefs - 2 - i)));
    }

    return res;
  }

  /** 
   * @param pp {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKeys  the keys
   * @return Node sensitivity of second derivative value at x=xKeys
   */
  public DoubleMatrix1D[] differentiateTwiceNodeSensitivity(final PiecewisePolynomialResultsWithSensitivity pp, final double[] xKeys) {
    ArgChecker.notNull(pp, "null pp");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    final int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 3, "Polynomial degree is too low");
    final int nIntervals = pp.getNumberOfIntervals();

    final DoubleMatrix2D[] diffSense = new DoubleMatrix2D[nIntervals];
    final DoubleMatrix2D[] senseMat = pp.getCoefficientSensitivityAll();
    final int nData = senseMat[0].getNumberOfColumns();
    for (int i = 0; i < nIntervals; ++i) {
      final double[][] tmp = new double[nCoefs - 2][nData];
      for (int j = 0; j < nCoefs - 2; ++j) {
        for (int k = 0; k < nData; ++k) {
          tmp[j][k] = (nCoefs - 1 - j) * (nCoefs - 2 - j) * senseMat[i].getData()[j][k];
        }
      }
      diffSense[i] = new DoubleMatrix2D(tmp);
    }

    PiecewisePolynomialResultsWithSensitivity ppDiff = new PiecewisePolynomialResultsWithSensitivity(pp.getKnots(), pp.getCoefMatrix(), nCoefs - 2, pp.getDimensions(), diffSense);
    return nodeSensitivity(ppDiff, xKeys);
  }
}
