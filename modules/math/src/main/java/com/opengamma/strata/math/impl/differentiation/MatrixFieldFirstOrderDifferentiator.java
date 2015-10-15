/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Matrix field first order differentiator.
 */
public class MatrixFieldFirstOrderDifferentiator
    implements Differentiator<DoubleMatrix1D, DoubleMatrix2D, DoubleMatrix2D[]> {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();
  private static final double DEFAULT_EPS = 1e-5;

  private final double eps;
  private final double twoEps;
  private final double oneOverTwpEps;

  /**
   * Creates an instance using the default value of eps (10<sup>-5</sup>).
   */
  public MatrixFieldFirstOrderDifferentiator() {
    eps = DEFAULT_EPS;
    twoEps = 2 * DEFAULT_EPS;
    oneOverTwpEps = 1.0 / twoEps;
  }

  /**
   * Creates an instance specifying the value of eps.
   * 
   * @param eps  the step size used to approximate the derivative
   */
  public MatrixFieldFirstOrderDifferentiator(double eps) {
    ArgChecker.isTrue(eps > 1e-15, "eps of {} is below machine tolerance of 1e-15. Please choose a higher value", eps);
    this.eps = eps;
    this.twoEps = 2 * eps;
    this.oneOverTwpEps = 1.0 / twoEps;
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<DoubleMatrix1D, DoubleMatrix2D[]> differentiate(
      Function1D<DoubleMatrix1D, DoubleMatrix2D> function) {

    ArgChecker.notNull(function, "function");
    return new Function1D<DoubleMatrix1D, DoubleMatrix2D[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D[] evaluate(DoubleMatrix1D x) {
        ArgChecker.notNull(x, "x");
        int n = x.size();

        DoubleMatrix2D[] res = new DoubleMatrix2D[n];
        for (int i = 0; i < n; i++) {
          double xi = x.get(i);
          DoubleMatrix2D up = function.evaluate(x.with(i, xi + eps));
          DoubleMatrix2D down = function.evaluate(x.with(i, xi - eps));
          res[i] = (DoubleMatrix2D) MA.scale(MA.subtract(up, down), oneOverTwpEps); //TODO have this in one operation
        }
        return res;
      }
    };
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<DoubleMatrix1D, DoubleMatrix2D[]> differentiate(
      Function1D<DoubleMatrix1D, DoubleMatrix2D> function,
      Function1D<DoubleMatrix1D, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(domain, "domain");

    double[] wFwd = new double[] {-3. / twoEps, 4. / twoEps, -1. / twoEps};
    double[] wCent = new double[] {-1. / twoEps, 0., 1. / twoEps};
    double[] wBack = new double[] {1. / twoEps, -4. / twoEps, 3. / twoEps};

    return new Function1D<DoubleMatrix1D, DoubleMatrix2D[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D[] evaluate(DoubleMatrix1D x) {
        ArgChecker.notNull(x, "x");
        ArgChecker.isTrue(domain.evaluate(x), "point {} is not in the function domain", x.toString());

        int n = x.size();
        DoubleMatrix2D[] y = new DoubleMatrix2D[3];
        DoubleMatrix2D[] res = new DoubleMatrix2D[n];
        double[] w;
        for (int i = 0; i < n; i++) {
          double xi = x.get(i);
          DoubleMatrix1D xPlusOneEps = x.with(i, xi + eps);
          DoubleMatrix1D xMinusOneEps = x.with(i, xi - eps);
          if (!domain.evaluate(xPlusOneEps)) {
            DoubleMatrix1D xMinusTwoEps = x.with(i, xi - twoEps);
            if (!domain.evaluate(xMinusTwoEps)) {
              throw new MathException("cannot get derivative at point " + x.toString() + " in direction " + i);
            }
            y[0] = function.evaluate(xMinusTwoEps);
            y[2] = function.evaluate(x);
            y[1] = function.evaluate(xMinusOneEps);
            w = wBack;
          } else {
            if (!domain.evaluate(xMinusOneEps)) {
              y[1] = function.evaluate(xPlusOneEps);
              y[0] = function.evaluate(x);
              y[2] = function.evaluate(x.with(i, xi + twoEps));
              w = wFwd;
            } else {
              y[2] = function.evaluate(xPlusOneEps);
              y[0] = function.evaluate(xMinusOneEps);
              w = wCent;
            }
          }
          res[i] = (DoubleMatrix2D) MA.add(MA.scale(y[0], w[0]), MA.scale(y[2], w[2]));
          if (w[1] != 0) {
            res[i] = (DoubleMatrix2D) MA.add(res[i], MA.scale(y[1], w[1]));
          }
        }
        return res;
      }
    };

  }

}
