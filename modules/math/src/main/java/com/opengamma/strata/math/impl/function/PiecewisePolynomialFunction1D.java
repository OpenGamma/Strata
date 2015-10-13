/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

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
  public DoubleMatrix1D evaluate(PiecewisePolynomialResult pp, double xKey) {
    ArgChecker.notNull(pp, "pp");

    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    double[] knots = pp.getKnots().getData();
    int nKnots = knots.length;
    DoubleMatrix2D coefMatrix = pp.getCoefMatrix();
    int dim = pp.getDimensions();

    double[] res = new double[dim];

    int indicator = FunctionUtils.getLowerBoundIndex(knots, xKey);
    if (indicator == nKnots - 1) {
      indicator--; //there is 1 less interval that knots 
    }

    for (int j = 0; j < dim; ++j) {
      double[] coefs = coefMatrix.row(dim * indicator + j).getData();
      res[j] = getValue(coefs, xKey, knots[indicator]);

      ArgChecker.isFalse(Double.isInfinite(res[j]), "Too large input");
      ArgChecker.isFalse(Double.isNaN(res[j]), "Too large input");
    }

    return new DoubleMatrix1D(res);
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
  public DoubleMatrix2D evaluate(PiecewisePolynomialResult pp, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.notNull(xKeys, "xKeys");

    int keyLength = xKeys.length;
    for (int i = 0; i < keyLength; ++i) {
      ArgChecker.isFalse(Double.isNaN(xKeys[i]), "xKeys containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xKeys[i]), "xKeys containing Infinity");
    }

    double[] knots = pp.getKnots().getData();
    int nKnots = knots.length;
    DoubleMatrix2D coefMatrix = pp.getCoefMatrix();
    int dim = pp.getDimensions();

    double[][] res = new double[dim][keyLength];

    for (int k = 0; k < dim; ++k) {
      for (int j = 0; j < keyLength; ++j) {
        int indicator = 0;
        if (xKeys[j] < knots[1]) {
          indicator = 0;
        } else {
          for (int i = 1; i < nKnots - 1; ++i) {
            if (knots[i] <= xKeys[j]) {
              indicator = i;
            }
          }
        }
        double[] coefs = coefMatrix.row(dim * indicator + k).getData();
        res[k][j] = getValue(coefs, xKeys[j], knots[indicator]);
        ArgChecker.isFalse(Double.isInfinite(res[k][j]), "Too large input");
        ArgChecker.isFalse(Double.isNaN(res[k][j]), "Too large input");
      }
    }

    return new DoubleMatrix2D(res);
  }

  /**
   * Evaluates the function.
   * 
   * @param pp  the PiecewisePolynomialResult
   * @param xKeys  the key
   * @return the values of piecewise polynomial functions at xKeys
   *  When _dim in PiecewisePolynomialResult is greater than 1, i.e., the struct contains
   *  multiple piecewise polynomials, one element of return vector of DoubleMatrix2D
   *  corresponds to each piecewise polynomial
   */
  public DoubleMatrix2D[] evaluate(PiecewisePolynomialResult pp, double[][] xKeys) {
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

    double[] knots = pp.getKnots().getData();
    int nKnots = knots.length;
    DoubleMatrix2D coefMatrix = pp.getCoefMatrix();
    int dim = pp.getDimensions();

    double[][][] res = new double[dim][keyDim][keyLength];

    for (int k = 0; k < dim; ++k) {
      for (int l = 0; l < keyDim; ++l) {
        for (int j = 0; j < keyLength; ++j) {
          int indicator = 0;
          if (xKeys[l][j] < knots[1]) {
            indicator = 0;
          } else {
            for (int i = 1; i < nKnots - 1; ++i) {
              if (knots[i] <= xKeys[l][j]) {
                indicator = i;
              }
            }
          }

          double[] coefs = coefMatrix.row(dim * indicator + k).getData();
          res[k][l][j] = getValue(coefs, xKeys[l][j], knots[indicator]);
          ArgChecker.isFalse(Double.isInfinite(res[k][l][j]), "Too large input");
          ArgChecker.isFalse(Double.isNaN(res[k][l][j]), "Too large input");
        }
      }
    }

    DoubleMatrix2D[] resMat = new DoubleMatrix2D[dim];
    for (int i = 0; i < dim; ++i) {
      resMat[i] = new DoubleMatrix2D(res[i]);
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
  public DoubleMatrix1D differentiate(PiecewisePolynomialResult pp, double xKey) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 2, "polynomial degree < 1");

    double[][] coefs = pp.getCoefMatrix().getData();
    DoubleMatrix1D knots = pp.getKnots();

    int nKnots = pp.getNumberOfIntervals() + 1;
    int nCoefs = pp.getOrder();
    int dim = pp.getDimensions();

    double[][] res = new double[dim * (nKnots - 1)][nCoefs - 1];
    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      for (int j = 0; j < nCoefs - 1; ++j) {
        res[i][j] = coefs[i][j] * (nCoefs - j - 1);
      }
    }

    PiecewisePolynomialResult ppDiff =
        new PiecewisePolynomialResult(knots, new DoubleMatrix2D(res), nCoefs - 1, pp.getDimensions());

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
  public DoubleMatrix2D differentiate(PiecewisePolynomialResult pp, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 2, "polynomial degree < 1");

    double[][] coefs = pp.getCoefMatrix().getData();
    DoubleMatrix1D knots = pp.getKnots();

    int nKnots = pp.getNumberOfIntervals() + 1;
    int nCoefs = pp.getOrder();
    int dim = pp.getDimensions();

    double[][] res = new double[dim * (nKnots - 1)][nCoefs - 1];
    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      for (int j = 0; j < nCoefs - 1; ++j) {
        res[i][j] = coefs[i][j] * (nCoefs - j - 1);
      }
    }

    PiecewisePolynomialResult ppDiff =
        new PiecewisePolynomialResult(knots, new DoubleMatrix2D(res), nCoefs - 1, pp.getDimensions());

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
  public DoubleMatrix1D differentiateTwice(PiecewisePolynomialResult pp, double xKey) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 3, "polynomial degree < 2");

    double[][] coefs = pp.getCoefMatrix().getData();
    DoubleMatrix1D knots = pp.getKnots();

    int nKnots = pp.getNumberOfIntervals() + 1;
    int nCoefs = pp.getOrder();
    int dim = pp.getDimensions();

    double[][] res = new double[dim * (nKnots - 1)][nCoefs - 2];
    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      for (int j = 0; j < nCoefs - 2; ++j) {
        res[i][j] = coefs[i][j] * (nCoefs - j - 1) * (nCoefs - j - 2);
      }
    }

    PiecewisePolynomialResult ppDiff =
        new PiecewisePolynomialResult(knots, new DoubleMatrix2D(res), nCoefs - 1, pp.getDimensions());

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
  public DoubleMatrix2D differentiateTwice(PiecewisePolynomialResult pp, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.isFalse(pp.getOrder() < 3, "polynomial degree < 2");

    double[][] coefs = pp.getCoefMatrix().getData();
    DoubleMatrix1D knots = pp.getKnots();

    int nKnots = pp.getNumberOfIntervals() + 1;
    int nCoefs = pp.getOrder();
    int dim = pp.getDimensions();

    double[][] res = new double[dim * (nKnots - 1)][nCoefs - 2];
    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < dim * (nKnots - 1); ++i) {
      for (int j = 0; j < nCoefs - 2; ++j) {
        res[i][j] = coefs[i][j] * (nCoefs - j - 1) * (nCoefs - j - 2);
      }
    }

    PiecewisePolynomialResult ppDiff =
        new PiecewisePolynomialResult(knots, new DoubleMatrix2D(res), nCoefs - 1, pp.getDimensions());

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

    double[] knots = pp.getKnots().toArray();
    int nCoefs = pp.getOrder();
    int nKnots = pp.getNumberOfIntervals() + 1;
    double[][] coefMatrix = pp.getCoefMatrix().getData();

    double[][] res = new double[nKnots - 1][nCoefs + 1];
    for (int i = 0; i < nKnots - 1; ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < nKnots - 1; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        res[i][j] = coefMatrix[i][j] / (nCoefs - j);
      }
    }

    double[] constTerms = new double[nKnots - 1];
    Arrays.fill(constTerms, 0.);

    int indicator = 0;
    if (initialKey <= knots[1]) {
      indicator = 0;
    } else {
      for (int i = 1; i < nKnots - 1; ++i) {
        if (knots[i] < initialKey) {
          indicator = i;
        }
      }
    }

    double sum = getValue(res[indicator], initialKey, knots[indicator]);
    for (int i = indicator; i < nKnots - 2; ++i) {
      constTerms[i + 1] = constTerms[i] + getValue(res[i], knots[i + 1], knots[i]) - sum;
      sum = 0.;
    }
    constTerms[indicator] = -getValue(res[indicator], initialKey, knots[indicator]);
    for (int i = indicator - 1; i > -1; --i) {
      constTerms[i] = constTerms[i + 1] - getValue(res[i], knots[i + 1], knots[i]);
    }
    for (int i = 0; i < nKnots - 1; ++i) {
      res[i][nCoefs] = constTerms[i];
    }
    PiecewisePolynomialResult ppInt =
        new PiecewisePolynomialResult(pp.getKnots(), new DoubleMatrix2D(res), nCoefs + 1, 1);

    return evaluate(ppInt, xKey).toArray()[0];
  }

  /**
   * Integration.
   * 
   * @param pp the PiecewisePolynomialResult
   * @param initialKey  the initial key
   * @param xKeys  the keys
   * @return the integral of piecewise polynomial between initialKey and xKeys 
   */
  public DoubleMatrix1D integrate(PiecewisePolynomialResult pp, double initialKey, double[] xKeys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.notNull(xKeys, "xKeys");

    ArgChecker.isFalse(Double.isNaN(initialKey), "initialKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(initialKey), "initialKey containing Infinity");
    ArgChecker.isTrue(pp.getDimensions() == 1, "Dimension should be 1");

    double[] knots = pp.getKnots().toArray();
    int nCoefs = pp.getOrder();
    int nKnots = pp.getNumberOfIntervals() + 1;
    double[][] coefMatrix = pp.getCoefMatrix().getData();

    double[][] res = new double[nKnots - 1][nCoefs + 1];
    for (int i = 0; i < nKnots - 1; ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < nKnots - 1; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        res[i][j] = coefMatrix[i][j] / (nCoefs - j);
      }
    }

    double[] constTerms = new double[nKnots - 1];
    Arrays.fill(constTerms, 0.);

    int indicator = 0;
    if (initialKey <= knots[1]) {
      indicator = 0;
    } else {
      for (int i = 1; i < nKnots - 1; ++i) {
        if (knots[i] < initialKey) {
          indicator = i;
        }
      }
    }

    double sum = getValue(res[indicator], initialKey, knots[indicator]);
    for (int i = indicator; i < nKnots - 2; ++i) {
      constTerms[i + 1] = constTerms[i] + getValue(res[i], knots[i + 1], knots[i]) - sum;
      sum = 0.;
    }

    constTerms[indicator] = -getValue(res[indicator], initialKey, knots[indicator]);
    for (int i = indicator - 1; i > -1; --i) {
      constTerms[i] = constTerms[i + 1] - getValue(res[i], knots[i + 1], knots[i]);
    }
    for (int i = 0; i < nKnots - 1; ++i) {
      res[i][nCoefs] = constTerms[i];
    }

    PiecewisePolynomialResult ppInt =
        new PiecewisePolynomialResult(pp.getKnots(), new DoubleMatrix2D(res), nCoefs + 1, 1);

    return new DoubleMatrix1D(evaluate(ppInt, xKeys).getData()[0]);
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
