/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;

/**
 * Give a struct {@link PiecewisePolynomialResult}, Compute value, first derivative
 * and integral of piecewise polynomial function.
 */
public class PiecewisePolynomialFunction1D {

  /**
   * Creates an instance.
   */
  public PiecewisePolynomialFunction1D() {
  }

  //-------------------------------------------------------------------------
  /**
   * Evaluates the function.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKey  the key
   * @return the values of piecewise polynomial functions at xKey 
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple splines, an element in the return values corresponds to each spline 
   */
  public DoubleArray evaluate(PiecewisePolynomialResult pp, double xKey) {
    ArgChecker.notNull(pp, "pp");

    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    DoubleArray knots = pp.getKnots();
    int nKnots = knots.size();
    DoubleMatrix coefMatrix = pp.getCoefMatrix();

    // check for 1 less interval that knots 
    int lowerBound = FunctionUtils.getLowerBoundIndex(knots, xKey);
    int indicator = lowerBound == nKnots - 1 ? lowerBound - 1 : lowerBound;

    return DoubleArray.of(pp.getDimensions(), i -> {
      DoubleArray coefs = coefMatrix.row(pp.getDimensions() * indicator + i);
      double res = getValue(coefs, xKey, knots.get(indicator));
      ArgChecker.isFalse(Double.isInfinite(res), "Too large input");
      ArgChecker.isFalse(Double.isNaN(res), "Too large input");
      return res;
    });
  }

  /**
   * Evaluates the function.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKeys  the key
   * @return the values of piecewise polynomial functions at xKeys 
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple piecewise polynomials, a row vector of return value corresponds to each piecewise polynomial
   */
  public DoubleMatrix evaluate(PiecewisePolynomialResult pp, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.notNull(xKeys, "xKeys");

    int keyLength = xKeys.length;
    for (int i = 0; i < keyLength; ++i) {
      ArgChecker.isFalse(Double.isNaN(xKeys[i]), "xKeys containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xKeys[i]), "xKeys containing Infinity");
    }

    DoubleArray knots = pp.getKnots();
    int nKnots = knots.size();
    DoubleMatrix coefMatrix = pp.getCoefMatrix();
    int dim = pp.getDimensions();

    double[][] res = new double[dim][keyLength];

    for (int k = 0; k < dim; ++k) {
      for (int j = 0; j < keyLength; ++j) {
        int indicator = 0;
        if (xKeys[j] < knots.get(1)) {
          indicator = 0;
        } else {
          for (int i = 1; i < nKnots - 1; ++i) {
            if (knots.get(i) <= xKeys[j]) {
              indicator = i;
            }
          }
        }
        DoubleArray coefs = coefMatrix.row(dim * indicator + k);
        res[k][j] = getValue(coefs, xKeys[j], knots.get(indicator));
        ArgChecker.isFalse(Double.isInfinite(res[k][j]), "Too large input");
        ArgChecker.isFalse(Double.isNaN(res[k][j]), "Too large input");
      }
    }

    return DoubleMatrix.copyOf(res);
  }

  /**
   * Evaluates the function.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKeys  the key
   * @return the values of piecewise polynomial functions at xKeys
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple piecewise polynomials, one element of return vector of DoubleMatrix
   *  corresponds to each piecewise polynomial
   */
  public DoubleMatrix[] evaluate(PiecewisePolynomialResult pp, double[][] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.notNull(xKeys, "xKeys");

    int keyLength = xKeys[0].length;
    int keyDim = xKeys.length;
    for (int j = 0; j < keyDim; ++j) {
      for (int i = 0; i < keyLength; ++i) {
        ArgChecker.isFalse(Double.isNaN(xKeys[j][i]), "xKeys containing NaN");
        ArgChecker.isFalse(Double.isInfinite(xKeys[j][i]), "xKeys containing Infinity");
      }
    }

    DoubleArray knots = pp.getKnots();
    int nKnots = knots.size();
    DoubleMatrix coefMatrix = pp.getCoefMatrix();
    int dim = pp.getDimensions();

    double[][][] res = new double[dim][keyDim][keyLength];

    for (int k = 0; k < dim; ++k) {
      for (int l = 0; l < keyDim; ++l) {
        for (int j = 0; j < keyLength; ++j) {
          int indicator = 0;
          if (xKeys[l][j] < knots.get(1)) {
            indicator = 0;
          } else {
            for (int i = 1; i < nKnots - 1; ++i) {
              if (knots.get(i) <= xKeys[l][j]) {
                indicator = i;
              }
            }
          }

          DoubleArray coefs = coefMatrix.row(dim * indicator + k);
          res[k][l][j] = getValue(coefs, xKeys[l][j], knots.get(indicator));
          ArgChecker.isFalse(Double.isInfinite(res[k][l][j]), "Too large input");
          ArgChecker.isFalse(Double.isNaN(res[k][l][j]), "Too large input");
        }
      }
    }

    DoubleMatrix[] resMat = new DoubleMatrix[dim];
    for (int i = 0; i < dim; ++i) {
      resMat[i] = DoubleMatrix.copyOf(res[i]);
    }
    return resMat;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the first derivatives.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKey  the key
   * @return the first derivatives of piecewise polynomial functions at xKey 
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple piecewise polynomials, an element in the return values corresponds to each piecewise polynomial 
   */
  public DoubleArray differentiate(PiecewisePolynomialResult pp, double xKey) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 2, "polynomial degree < 1");

    DoubleArray knots = pp.getKnots();
    int nCoefs = pp.getOrder();
    int rowCount = pp.getDimensions() * pp.getNumberOfIntervals();
    int colCount = nCoefs - 1;
    DoubleMatrix coef = DoubleMatrix.of(
        rowCount,
        colCount,
        (i, j) -> pp.getCoefMatrix().get(i, j) * (nCoefs - j - 1));
    PiecewisePolynomialResult ppDiff = new PiecewisePolynomialResult(knots, coef, colCount, pp.getDimensions());
    return evaluate(ppDiff, xKey);
  }

  /**
   * Finds the first derivatives.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKeys  the key
   * @return the first derivatives of piecewise polynomial functions at xKeys 
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple piecewise polynomials, a row vector of return value corresponds to each piecewise polynomial
   */
  public DoubleMatrix differentiate(PiecewisePolynomialResult pp, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 2, "polynomial degree < 1");

    DoubleArray knots = pp.getKnots();
    int nCoefs = pp.getOrder();
    int rowCount = pp.getDimensions() * pp.getNumberOfIntervals();
    int colCount = nCoefs - 1;
    DoubleMatrix coef = DoubleMatrix.of(
        rowCount,
        colCount,
        (i, j) -> pp.getCoefMatrix().get(i, j) * (nCoefs - j - 1));
    PiecewisePolynomialResult ppDiff = new PiecewisePolynomialResult(knots, coef, colCount, pp.getDimensions());
    return evaluate(ppDiff, xKeys);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the second derivatives.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKey  the key
   * @return the second derivatives of piecewise polynomial functions at xKey 
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple piecewise polynomials, an element in the return values corresponds to each piecewise polynomial 
   */
  public DoubleArray differentiateTwice(PiecewisePolynomialResult pp, double xKey) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 3, "polynomial degree < 2");

    DoubleArray knots = pp.getKnots();
    int nCoefs = pp.getOrder();
    int rowCount = pp.getDimensions() * pp.getNumberOfIntervals();
    int colCount = nCoefs - 2;
    DoubleMatrix coef = DoubleMatrix.of(
        rowCount,
        colCount,
        (i, j) -> pp.getCoefMatrix().get(i, j) * (nCoefs - j - 1) * (nCoefs - j - 2));
    PiecewisePolynomialResult ppDiff = new PiecewisePolynomialResult(knots, coef, nCoefs - 1, pp.getDimensions());
    return evaluate(ppDiff, xKey);
  }

  /**
   * Finds the second derivatives.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKeys  the key
   * @return the second derivatives of piecewise polynomial functions at xKeys 
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple piecewise polynomials, a row vector of return value corresponds to each piecewise polynomial
   */
  public DoubleMatrix differentiateTwice(PiecewisePolynomialResult pp, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 3, "polynomial degree < 2");

    DoubleArray knots = pp.getKnots();
    int nCoefs = pp.getOrder();
    int rowCount = pp.getDimensions() * pp.getNumberOfIntervals();
    int colCount = nCoefs - 2;
    DoubleMatrix coef = DoubleMatrix.of(
        rowCount,
        colCount,
        (i, j) -> pp.getCoefMatrix().get(i, j) * (nCoefs - j - 1) * (nCoefs - j - 2));
    PiecewisePolynomialResult ppDiff = new PiecewisePolynomialResult(knots, coef, nCoefs - 1, pp.getDimensions());
    return evaluate(ppDiff, xKeys);
  }

  //-------------------------------------------------------------------------
  /**
   * Integration.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param initialKey  the initial key
   * @param xKey  the key
   * @return the integral of piecewise polynomial between initialKey and xKey 
   */
  public double integrate(PiecewisePolynomialResult pp, double initialKey, double xKey) {
    ArgChecker.notNull(pp, "pp");

    ArgChecker.isFalse(Double.isNaN(initialKey), "initialKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(initialKey), "initialKey containing Infinity");
    ArgChecker.isTrue(pp.getDimensions() == 1, "Dimension should be 1");

    DoubleArray knots = pp.getKnots();
    int nCoefs = pp.getOrder();
    int nKnots = pp.getNumberOfIntervals() + 1;

    int rowCount = nKnots - 1;
    int colCount = nCoefs + 1;
    double[][] res = new double[rowCount][colCount];
    for (int i = 0; i < rowCount; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        res[i][j] = pp.getCoefMatrix().get(i, j) / (nCoefs - j);
      }
    }

    double[] constTerms = new double[rowCount];
    int indicator = 0;
    if (initialKey <= knots.get(1)) {
      indicator = 0;
    } else {
      for (int i = 1; i < rowCount; ++i) {
        if (knots.get(i) < initialKey) {
          indicator = i;
        }
      }
    }

    double sum = getValue(res[indicator], initialKey, knots.get(indicator));
    for (int i = indicator; i < nKnots - 2; ++i) {
      constTerms[i + 1] = constTerms[i] + getValue(res[i], knots.get(i + 1), knots.get(i)) - sum;
      sum = 0d;
    }
    constTerms[indicator] = -getValue(res[indicator], initialKey, knots.get(indicator));
    for (int i = indicator - 1; i > -1; --i) {
      constTerms[i] = constTerms[i + 1] - getValue(res[i], knots.get(i + 1), knots.get(i));
    }
    for (int i = 0; i < rowCount; ++i) {
      res[i][nCoefs] = constTerms[i];
    }
    PiecewisePolynomialResult ppInt =
        new PiecewisePolynomialResult(pp.getKnots(), DoubleMatrix.copyOf(res), colCount, 1);

    return evaluate(ppInt, xKey).get(0);
  }

  /**
   * Integration.
   * 
   * @param pp the PiecewisePolynomialResult
   * @param initialKey  the initial key
   * @param xKeys  the keys
   * @return the integral of piecewise polynomial between initialKey and xKeys 
   */
  public DoubleArray integrate(PiecewisePolynomialResult pp, double initialKey, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.notNull(xKeys, "xKeys");

    ArgChecker.isFalse(Double.isNaN(initialKey), "initialKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(initialKey), "initialKey containing Infinity");
    ArgChecker.isTrue(pp.getDimensions() == 1, "Dimension should be 1");

    DoubleArray knots = pp.getKnots();
    int nCoefs = pp.getOrder();
    int nKnots = pp.getNumberOfIntervals() + 1;

    int rowCount = nKnots - 1;
    int colCount = nCoefs + 1;
    double[][] res = new double[rowCount][colCount];
    for (int i = 0; i < rowCount; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        res[i][j] = pp.getCoefMatrix().get(i, j) / (nCoefs - j);
      }
    }

    double[] constTerms = new double[rowCount];
    int indicator = 0;
    if (initialKey <= knots.get(1)) {
      indicator = 0;
    } else {
      for (int i = 1; i < rowCount; ++i) {
        if (knots.get(i) < initialKey) {
          indicator = i;
        }
      }
    }

    double sum = getValue(res[indicator], initialKey, knots.get(indicator));
    for (int i = indicator; i < nKnots - 2; ++i) {
      constTerms[i + 1] = constTerms[i] + getValue(res[i], knots.get(i + 1), knots.get(i)) - sum;
      sum = 0.;
    }

    constTerms[indicator] = -getValue(res[indicator], initialKey, knots.get(indicator));
    for (int i = indicator - 1; i > -1; --i) {
      constTerms[i] = constTerms[i + 1] - getValue(res[i], knots.get(i + 1), knots.get(i));
    }
    for (int i = 0; i < rowCount; ++i) {
      res[i][nCoefs] = constTerms[i];
    }

    PiecewisePolynomialResult ppInt =
        new PiecewisePolynomialResult(pp.getKnots(), DoubleMatrix.copyOf(res), colCount, 1);

    return evaluate(ppInt, xKeys).row(0);
  }

  //-------------------------------------------------------------------------
  /**
   * Evaluates the function and its first derivative.
   * <p>
   * The dimension of {@code PiecewisePolynomialResult} must be 1.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKey  the key
   * @return the value and derivative
   */
  public ValueDerivatives evaluateAndDifferentiate(PiecewisePolynomialResult pp, double xKey) {
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
    DoubleArray coefs = pp.getCoefMatrix().row(interval);
    int nCoefs = coefs.size();

    double resValue = coefs.get(0);
    double resDeriv = coefs.get(0) * (nCoefs - 1);
    for (int i = 1; i < nCoefs - 1; i++) {
      resValue *= s;
      resValue += coefs.get(i);
      resDeriv *= s;
      resDeriv += coefs.get(i) * (nCoefs - i - 1);
      ArgChecker.isFalse(Double.isInfinite(resValue), "Too large input");
      ArgChecker.isFalse(Double.isNaN(resValue), "Too large input");
    }
    resValue *= s;
    resValue += coefs.get(nCoefs - 1);

    return ValueDerivatives.of(resValue, DoubleArray.of(resDeriv));
  }

  //-------------------------------------------------------------------------
  /**
   * @param coefs  {a_n,a_{n-1},...} of f(x) = a_n x^{n} + a_{n-1} x^{n-1} + ....
   * @param x  the x-value
   * @param leftknot  the knot specifying underlying interpolation function
   * @return the value of the underlying interpolation function at the value of x
   */
  protected double getValue(DoubleArray coefs, double x, double leftknot) {
    // needs to delegate as method is protected
    return getValue(coefs.toArrayUnsafe(), x, leftknot);
  }

  /**
   * @param coefs  {a_n,a_{n-1},...} of f(x) = a_n x^{n} + a_{n-1} x^{n-1} + ....
   * @param x  the x-value
   * @param leftknot  the knot specifying underlying interpolation function
   * @return the value of the underlying interpolation function at the value of x
   */
  protected double getValue(double[] coefs, double x, double leftknot) {
    int nCoefs = coefs.length;
    double s = x - leftknot;
    double res = coefs[0];
    for (int i = 1; i < nCoefs; i++) {
      res *= s;
      res += coefs[i];
    }
    return res;
  }

}
