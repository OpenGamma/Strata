/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Give a class {@link PiecewisePolynomialResultsWithSensitivity}, compute node sensitivity of
 * function value, first derivative value and second derivative value.
 */
public class PiecewisePolynomialWithSensitivityFunction1D extends PiecewisePolynomialFunction1D {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  /**
   * Finds the node sensitivity.
   * 
   * @param pp  the {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKey  the key
   * @return Node sensitivity value at x=xKey
   */
  public DoubleArray nodeSensitivity(PiecewisePolynomialResultsWithSensitivity pp, double xKey) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }

    DoubleArray knots = pp.getKnots();
    int nKnots = knots.size();
    int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
    if (interval == nKnots - 1) {
      interval--; // there is 1 less interval that knots
    }

    double s = xKey - knots.get(interval);
    DoubleMatrix a = pp.getCoefficientSensitivity(interval);
    int nCoefs = a.rowCount();

    DoubleArray res = a.row(0);
    for (int i = 1; i < nCoefs; i++) {
      res = (DoubleArray) MA.scale(res, s);
      res = (DoubleArray) MA.add(res, a.row(i));
    }

    return res;
  }

  /**
   * Finds the node sensitivity.
   * 
   * @param pp  the {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKeys  the keys
   * @return the node sensitivity value at x=xKeys
   */
  public DoubleArray[] nodeSensitivity(PiecewisePolynomialResultsWithSensitivity pp, double[] xKeys) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.notNull(xKeys, "null xKeys");
    int nKeys = xKeys.length;
    DoubleArray[] res = new DoubleArray[nKeys];

    for (int i = 0; i < nKeys; ++i) {
      ArgChecker.isFalse(Double.isNaN(xKeys[i]), "xKey containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xKeys[i]), "xKey containing Infinity");
    }
    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }

    DoubleArray knots = pp.getKnots();
    int nKnots = knots.size();

    for (int j = 0; j < nKeys; ++j) {
      double xKey = xKeys[j];
      int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
      if (interval == nKnots - 1) {
        interval--; // there is 1 less interval that knots
      }

      double s = xKey - knots.get(interval);
      DoubleMatrix a = pp.getCoefficientSensitivity(interval);
      int nCoefs = a.rowCount();

      res[j] = a.row(0);
      for (int i = 1; i < nCoefs; i++) {
        res[j] = (DoubleArray) MA.scale(res[j], s);
        res[j] = (DoubleArray) MA.add(res[j], a.row(i));
      }
    }

    return res;
  }

  //-------------------------------------------------------------------------
  /**
   * Differentiates the node sensitivity.
   * 
   * @param pp  the {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKey  the key
   * @return the node sensitivity of derivative value at x=xKey
   */
  public DoubleArray differentiateNodeSensitivity(PiecewisePolynomialResultsWithSensitivity pp, double xKey) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 2, "Polynomial degree is too low");

    DoubleArray knots = pp.getKnots();
    int nKnots = knots.size();
    int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
    if (interval == nKnots - 1) {
      interval--; // there is 1 less interval that knots
    }

    double s = xKey - knots.get(interval);
    DoubleMatrix a = pp.getCoefficientSensitivity(interval);

    DoubleArray res = (DoubleArray) MA.scale(a.row(0), nCoefs - 1);
    for (int i = 1; i < nCoefs - 1; i++) {
      res = (DoubleArray) MA.scale(res, s);
      res = (DoubleArray) MA.add(res, MA.scale(a.row(i), nCoefs - 1 - i));
    }

    return res;
  }

  /**
   * Differentiates the node sensitivity.
   * 
   * @param pp  the {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKeys  the keys
   * @return the node sensitivity of derivative value at x=xKeys
   */
  public DoubleArray[] differentiateNodeSensitivity(PiecewisePolynomialResultsWithSensitivity pp, double[] xKeys) {
    ArgChecker.notNull(pp, "null pp");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 2, "Polynomial degree is too low");
    int nIntervals = pp.getNumberOfIntervals();

    DoubleMatrix[] diffSense = new DoubleMatrix[nIntervals];
    DoubleMatrix[] senseMat = pp.getCoefficientSensitivityAll();
    int nData = senseMat[0].columnCount();
    for (int i = 0; i < nIntervals; ++i) {
      double[][] senseMatArray = senseMat[i].toArray();
      double[][] tmp = new double[nCoefs - 1][nData];
      for (int j = 0; j < nCoefs - 1; ++j) {
        for (int k = 0; k < nData; ++k) {
          tmp[j][k] = (nCoefs - 1 - j) * senseMatArray[j][k];
        }
      }
      diffSense[i] = DoubleMatrix.copyOf(tmp);
    }

    PiecewisePolynomialResultsWithSensitivity ppDiff = new PiecewisePolynomialResultsWithSensitivity(
        pp.getKnots(), pp.getCoefMatrix(), nCoefs - 1, pp.getDimensions(), diffSense);
    return nodeSensitivity(ppDiff, xKeys);
  }

  //-------------------------------------------------------------------------
  /**
   * Differentiates the node sensitivity.
   * 
   * @param pp  the {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKey  the key
   * @return the node sensitivity of second derivative value at x=xKey
   */
  public DoubleArray differentiateTwiceNodeSensitivity(PiecewisePolynomialResultsWithSensitivity pp, double xKey) {
    ArgChecker.notNull(pp, "null pp");
    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 3, "Polynomial degree is too low");

    DoubleArray knots = pp.getKnots();
    int nKnots = knots.size();
    int interval = FunctionUtils.getLowerBoundIndex(knots, xKey);
    if (interval == nKnots - 1) {
      interval--; // there is 1 less interval that knots
    }

    double s = xKey - knots.get(interval);
    DoubleMatrix a = pp.getCoefficientSensitivity(interval);

    DoubleArray res = (DoubleArray) MA.scale(a.row(0), (nCoefs - 1) * (nCoefs - 2));
    for (int i = 1; i < nCoefs - 2; i++) {
      res = (DoubleArray) MA.scale(res, s);
      res = (DoubleArray) MA.add(res, MA.scale(a.row(i), (nCoefs - 1 - i) * (nCoefs - 2 - i)));
    }

    return res;
  }

  /**
   * Differentiates the node sensitivity.
   * 
   * @param pp  the {@link PiecewisePolynomialResultsWithSensitivity}
   * @param xKeys  the keys
   * @return the node sensitivity of second derivative value at x=xKeys
   */
  public DoubleArray[] differentiateTwiceNodeSensitivity(PiecewisePolynomialResultsWithSensitivity pp, double[] xKeys) {
    ArgChecker.notNull(pp, "null pp");

    if (pp.getDimensions() > 1) {
      throw new UnsupportedOperationException();
    }
    int nCoefs = pp.getOrder();
    ArgChecker.isFalse(nCoefs < 3, "Polynomial degree is too low");
    int nIntervals = pp.getNumberOfIntervals();

    DoubleMatrix[] diffSense = new DoubleMatrix[nIntervals];
    DoubleMatrix[] senseMat = pp.getCoefficientSensitivityAll();
    int nData = senseMat[0].columnCount();
    for (int i = 0; i < nIntervals; ++i) {
      double[][] senseMatArray = senseMat[i].toArray();
      double[][] tmp = new double[nCoefs - 2][nData];
      for (int j = 0; j < nCoefs - 2; ++j) {
        for (int k = 0; k < nData; ++k) {
          tmp[j][k] = (nCoefs - 1 - j) * (nCoefs - 2 - j) * senseMatArray[j][k];
        }
      }
      diffSense[i] = DoubleMatrix.copyOf(tmp);
    }

    PiecewisePolynomialResultsWithSensitivity ppDiff =
        new PiecewisePolynomialResultsWithSensitivity(pp.getKnots(), pp.getCoefMatrix(), nCoefs - 2, pp.getDimensions(),
            diffSense);
    return nodeSensitivity(ppDiff, xKeys);
  }
}
