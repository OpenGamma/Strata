/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult2D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Computes value, first derivative and integral of piecewise polynomial function.
 */
public class PiecewisePolynomialFunction2D {

  /**
   * Creates an instance.
   */
  public PiecewisePolynomialFunction2D() {
  }

  //-------------------------------------------------------------------------
  /**
   * Evaluates the function.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Key  the first key
   * @param x1Key  the second key
   * @return the value of piecewise polynomial function in 2D at (x0Key, x1Key)
   */
  public double evaluate(PiecewisePolynomialResult2D pp, double x0Key, double x1Key) {
    ArgChecker.notNull(pp, "pp");

    ArgChecker.isFalse(Double.isNaN(x0Key), "x0Key containing NaN");
    ArgChecker.isFalse(Double.isInfinite(x0Key), "x0Key containing Infinity");
    ArgChecker.isFalse(Double.isNaN(x1Key), "x1Key containing NaN");
    ArgChecker.isFalse(Double.isInfinite(x1Key), "x1Key containing Infinity");

    double[] knots0 = pp.getKnots0().toArray();
    double[] knots1 = pp.getKnots1().toArray();
    int nKnots0 = knots0.length;
    int nKnots1 = knots1.length;

    int ind0 = 0;
    int ind1 = 0;

    for (int k = 1; k < nKnots0 - 1; ++k) {
      if (x0Key >= knots0[k]) {
        ind0 = k;
      }
    }

    for (int i = 1; i < nKnots1 - 1; ++i) {
      if (x1Key >= knots1[i]) {
        ind1 = i;
      }
    }
    double res = getValue(pp.getCoefs()[ind0][ind1], x0Key, x1Key, knots0[ind0], knots1[ind1]);

    ArgChecker.isFalse(Double.isInfinite(res), "Too large input");
    ArgChecker.isFalse(Double.isNaN(res), "Too large input");

    return res;
  }

  /**
   * Evaluates the function.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Keys  the first keys
   * @param x1Keys  the first keys
   * @return the values of piecewise polynomial function in 2D at (x0Keys_i, x1Keys_j)
   */
  public DoubleMatrix2D evaluate(PiecewisePolynomialResult2D pp, double[] x0Keys, double[] x1Keys) {
    ArgChecker.notNull(pp, "pp");
    ArgChecker.notNull(x0Keys, "x0Keys");
    ArgChecker.notNull(x1Keys, "x1Keys");

    int n0Keys = x0Keys.length;
    int n1Keys = x1Keys.length;

    for (int i = 0; i < n0Keys; ++i) {
      ArgChecker.isFalse(Double.isNaN(x0Keys[i]), "x0Keys containing NaN");
      ArgChecker.isFalse(Double.isInfinite(x0Keys[i]), "x0Keys containing Infinity");
    }
    for (int i = 0; i < n1Keys; ++i) {
      ArgChecker.isFalse(Double.isNaN(x1Keys[i]), "x1Keys containing NaN");
      ArgChecker.isFalse(Double.isInfinite(x1Keys[i]), "x1Keys containing Infinity");
    }

    double[] knots0 = pp.getKnots0().toArray();
    double[] knots1 = pp.getKnots1().toArray();
    int nKnots0 = knots0.length;
    int nKnots1 = knots1.length;

    double[][] res = new double[n0Keys][n1Keys];

    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        int ind0 = 0;
        int ind1 = 0;

        for (int k = 1; k < nKnots0 - 1; ++k) {
          if (x0Keys[i] >= knots0[k]) {
            ind0 = k;
          }
        }
        for (int k = 1; k < nKnots1 - 1; ++k) {
          if (x1Keys[j] >= knots1[k]) {
            ind1 = k;
          }
        }
        res[i][j] = getValue(pp.getCoefs()[ind0][ind1], x0Keys[i], x1Keys[j], knots0[ind0], knots1[ind1]);
        ArgChecker.isFalse(Double.isInfinite(res[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isNaN(res[i][j]), "Too large input");
      }
    }

    return new DoubleMatrix2D(res);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the first derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Key  the first key
   * @param x1Key  the second key
   * @return the value of first derivative of two-dimensional piecewise polynomial function
   *  with respect to x0 at (x0Keys_i, x1Keys_j)
   */
  public double differentiateX0(PiecewisePolynomialResult2D pp, double x0Key, double x1Key) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order0 < 2, "polynomial degree of x0 < 1");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0 - 1][order1];
        for (int k = 0; k < order0 - 1; ++k) {
          for (int l = 0; l < order1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order0 - k - 1);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0 - 1, order1});

    return evaluate(ppDiff, x0Key, x1Key);
  }

  /**
   * Finds the first derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Key  the first key
   * @param x1Key  the second key
   * @return the value of first derivative of two-dimensional piecewise polynomial function
   *  with respect to x1 at (x0Keys_i, x1Keys_j)
   */
  public double differentiateX1(PiecewisePolynomialResult2D pp, double x0Key, double x1Key) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order1 < 2, "polynomial degree of x1 < 1");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0][order1 - 1];
        for (int k = 0; k < order0; ++k) {
          for (int l = 0; l < order1 - 1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order1 - l - 1);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0, order1 - 1});

    return evaluate(ppDiff, x0Key, x1Key);
  }

  /**
   * Finds the first derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Keys  the first keys
   * @param x1Keys  the second keys
   * @return Values of first derivative of two-dimensional piecewise polynomial function
   *  with respect to x0 at (x0Keys_i, x1Keys_j)
   */
  public DoubleMatrix2D differentiateX0(PiecewisePolynomialResult2D pp, double[] x0Keys, double[] x1Keys) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order0 < 2, "polynomial degree of x0 < 1");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0 - 1][order1];
        for (int k = 0; k < order0 - 1; ++k) {
          for (int l = 0; l < order1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order0 - k - 1);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0 - 1, order1});

    return evaluate(ppDiff, x0Keys, x1Keys);
  }

  /**
   * Finds the first derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Keys  the first keys
   * @param x1Keys  the second keys
   * @return Values of first derivative of two-dimensional piecewise polynomial function
   *  with respect to x1 at (x0Keys_i, x1Keys_j)
   */
  public DoubleMatrix2D differentiateX1(PiecewisePolynomialResult2D pp, double[] x0Keys, double[] x1Keys) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order1 < 2, "polynomial degree of x1 < 1");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0][order1 - 1];
        for (int k = 0; k < order0; ++k) {
          for (int l = 0; l < order1 - 1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order1 - l - 1);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0, order1 - 1});

    return evaluate(ppDiff, x0Keys, x1Keys);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the cross derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Key  the first key
   * @param x1Key  the second key
   * @return the value of cross derivative of two-dimensional piecewise polynomial function at (x0Keys_i, x1Keys_j)
   */
  public double differentiateCross(PiecewisePolynomialResult2D pp, double x0Key, double x1Key) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order0 < 2, "polynomial degree of x0 < 1");
    ArgChecker.isFalse(order1 < 2, "polynomial degree of x1 < 1");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0 - 1][order1 - 1];
        for (int k = 0; k < order0 - 1; ++k) {
          for (int l = 0; l < order1 - 1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order1 - l - 1) * (order0 - k - 1);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0 - 1, order1 - 1});

    return evaluate(ppDiff, x0Key, x1Key);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the second derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Key  the first key
   * @param x1Key  the second key
   * @return the value of second derivative of two-dimensional piecewise polynomial function
   *  with respect to x0 at (x0Keys_i, x1Keys_j)
   */
  public double differentiateTwiceX0(PiecewisePolynomialResult2D pp, double x0Key, double x1Key) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order0 < 3, "polynomial degree of x0 < 2");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0 - 2][order1];
        for (int k = 0; k < order0 - 2; ++k) {
          for (int l = 0; l < order1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order0 - k - 1) * (order0 - k - 2);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0 - 2, order1});

    return evaluate(ppDiff, x0Key, x1Key);
  }

  /**
   * Finds the second derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Key  the first key
   * @param x1Key  the second key
   * @return the value of second derivative of two-dimensional piecewise polynomial function
   *  with respect to x1 at (x0Keys_i, x1Keys_j)
   */
  public double differentiateTwiceX1(PiecewisePolynomialResult2D pp, double x0Key, double x1Key) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order1 < 3, "polynomial degree of x1 < 2");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0][order1 - 2];
        for (int k = 0; k < order0; ++k) {
          for (int l = 0; l < order1 - 2; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order1 - l - 1) * (order1 - l - 2);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0, order1 - 2});

    return evaluate(ppDiff, x0Key, x1Key);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the cross derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Keys  the first keys
   * @param x1Keys  the second keys
   * @return the values of cross derivative of two-dimensional piecewise polynomial function at (x0Keys_i, x1Keys_j)
   */
  public DoubleMatrix2D differentiateCross(PiecewisePolynomialResult2D pp, double[] x0Keys, double[] x1Keys) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order0 < 2, "polynomial degree of x0 < 1");
    ArgChecker.isFalse(order1 < 2, "polynomial degree of x1 < 1");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0 - 1][order1 - 1];
        for (int k = 0; k < order0 - 1; ++k) {
          for (int l = 0; l < order1 - 1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order1 - l - 1) * (order0 - k - 1);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0 - 1, order1 - 1});

    return evaluate(ppDiff, x0Keys, x1Keys);
  }

  /**
   * Finds the second derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Keys  the first keys
   * @param x1Keys  the second keys
   * @return the values of second derivative of two-dimensional piecewise polynomial function
   *  with respect to x0 at (x0Keys_i, x1Keys_j)
   */
  public DoubleMatrix2D differentiateTwiceX0(PiecewisePolynomialResult2D pp, double[] x0Keys, double[] x1Keys) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order0 < 3, "polynomial degree of x0 < 2");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0 - 2][order1];
        for (int k = 0; k < order0 - 2; ++k) {
          for (int l = 0; l < order1; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order0 - k - 1) * (order0 - k - 2);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0 - 2, order1});

    return evaluate(ppDiff, x0Keys, x1Keys);
  }

  /**
   * Finds the second derivative.
   * 
   * @param pp  the PiecewisePolynomialResult2D
   * @param x0Keys  the first keys
   * @param x1Keys  the second keys
   * @return the values of second derivative of two-dimensional piecewise polynomial function
   *  with respect to x1 at (x0Keys_i, x1Keys_j)
   */
  public DoubleMatrix2D differentiateTwiceX1(PiecewisePolynomialResult2D pp, double[] x0Keys, double[] x1Keys) {
    ArgChecker.notNull(pp, "pp");
    int order0 = pp.getOrder()[0];
    int order1 = pp.getOrder()[1];
    ArgChecker.isFalse(order1 < 3, "polynomial degree of x1 < 2");

    DoubleMatrix1D knots0 = pp.getKnots0();
    DoubleMatrix1D knots1 = pp.getKnots1();
    int nKnots0 = knots0.getNumberOfElements();
    int nKnots1 = knots1.getNumberOfElements();
    DoubleMatrix2D[][] coefs = pp.getCoefs();

    DoubleMatrix2D[][] res = new DoubleMatrix2D[nKnots0][nKnots1];

    for (int i = 0; i < nKnots0 - 1; ++i) {
      for (int j = 0; j < nKnots1 - 1; ++j) {
        double[][] matTmp = new double[order0][order1 - 2];
        for (int k = 0; k < order0; ++k) {
          for (int l = 0; l < order1 - 2; ++l) {
            matTmp[k][l] = coefs[i][j].getData()[k][l] * (order1 - l - 1) * (order1 - l - 2);
          }
        }
        res[i][j] = new DoubleMatrix2D(matTmp);
      }
    }

    PiecewisePolynomialResult2D ppDiff = new PiecewisePolynomialResult2D(
        knots0, knots1, res, new int[] {order0, order1 - 2});

    return evaluate(ppDiff, x0Keys, x1Keys);
  }

  /**
   * @param coefMat 
   * @param x0 
   * @param x1 
   * @param leftKnot0 
   * @param leftKnot1 
   * @return sum_{i=0}^{order0-1} sum_{j=0}^{order1-1} coefMat_{ij} (x0-leftKnots0)^{order0-1-i} (x1-leftKnots1)^{order0-1-j}
   */
  private double getValue(DoubleMatrix2D coefMat, double x0, double x1, double leftKnot0, double leftKnot1) {

    int order0 = coefMat.getNumberOfRows();
    int order1 = coefMat.getNumberOfColumns();
    double x0Mod = x0 - leftKnot0;
    double x1Mod = x1 - leftKnot1;
    double res = 0.;

    for (int i = 0; i < order0; ++i) {
      for (int j = 0; j < order1; ++j) {
        res += coefMat.getData()[order0 - i - 1][order1 - j - 1] * Math.pow(x0Mod, i) * Math.pow(x1Mod, j);
      }
    }
    return res;
  }

}
