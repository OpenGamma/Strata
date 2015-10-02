/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Differentiates a scalar field (i.e. there is a scalar value for every point
 * in some vector space) with respect to the vector space using finite difference.
 * <p>
 * For a function $y = f(\mathbf{x})$ where $\mathbf{x}$ is a n-dimensional
 * vector and $y$ is a scalar, this class produces a gradient function
 * $\mathbf{g}(\mathbf{x})$, i.e. a function that returns the gradient for each
 * point $\mathbf{x}$, where $\mathbf{g}$ is the n-dimensional vector
 * $\frac{dy}{dx_i}$.
 */
public class ScalarFieldFirstOrderDifferentiator
    implements Differentiator<DoubleMatrix1D, Double, DoubleMatrix1D> {

  private static final double DEFAULT_EPS = 1e-5;
  private static final double MIN_EPS = Math.sqrt(Double.MIN_NORMAL);

  private final double eps;
  private final double twoEps;
  private final FiniteDifferenceType differenceType;

  /**
   * Creates an instance using the default values of differencing type (central) and eps (10<sup>-5</sup>).
   */
  public ScalarFieldFirstOrderDifferentiator() {
    this(FiniteDifferenceType.CENTRAL, DEFAULT_EPS);
  }

  /**
   * Creates an instance that approximates the derivative of a scalar function by finite difference.
   * <p>
   * If the size of the domain is very small or very large, consider re-scaling first.
   * If this value is too small, the result will most likely be dominated by noise.
   * Use around 10<sup>-5</sup> times the domain size.
   * 
   * @param differenceType  the type, forward, backward or central. In most situations, central is best
   * @param eps  the step size used to approximate the derivative
   */
  public ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType differenceType, double eps) {
    ArgChecker.notNull(differenceType, "differenceType");
    ArgChecker.isTrue(eps >= MIN_EPS,
        "eps of {} is too small. Please choose a value > {}, such as 1e-5*size of domain", eps, MIN_EPS);
    this.differenceType = differenceType;
    this.eps = eps;
    this.twoEps = 2 * eps;
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<DoubleMatrix1D, DoubleMatrix1D> differentiate(
      Function1D<DoubleMatrix1D, Double> function) {

    ArgChecker.notNull(function, "function");
    switch (differenceType) {
      case FORWARD:
        return new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
            ArgChecker.notNull(x, "x");
            int n = x.getNumberOfElements();
            double y = function.evaluate(x);
            double[] xData = x.getData();
            double oldValue;
            double[] res = new double[n];
            for (int i = 0; i < n; i++) {
              oldValue = xData[i];
              xData[i] += eps;
              res[i] = (function.evaluate(x) - y) / eps;
              xData[i] = oldValue;
            }
            return new DoubleMatrix1D(res);
          }
        };
      case CENTRAL:
        return new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
            ArgChecker.notNull(x, "x");
            int n = x.getNumberOfElements();
            double[] xData = x.getData();
            double oldValue;
            double up, down;
            double[] res = new double[n];
            for (int i = 0; i < n; i++) {
              oldValue = xData[i];
              xData[i] += eps;
              up = function.evaluate(x);
              xData[i] -= twoEps;
              down = function.evaluate(x);
              res[i] = (up - down) / twoEps;
              xData[i] = oldValue;
            }
            return new DoubleMatrix1D(res);
          }
        };
      case BACKWARD:
        return new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
            ArgChecker.notNull(x, "x");
            double y = function.evaluate(x);
            int n = x.getNumberOfElements();
            double[] xData = x.getData();
            double oldValue;
            double[] res = new double[n];
            for (int i = 0; i < n; i++) {
              oldValue = xData[i];
              xData[i] -= eps;
              res[i] = (y - function.evaluate(x)) / eps;
              xData[i] = oldValue;
            }
            return new DoubleMatrix1D(res);
          }
        };
      default:
        throw new IllegalArgumentException("Can only handle forward, backward and central differencing");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<DoubleMatrix1D, DoubleMatrix1D> differentiate(
      Function1D<DoubleMatrix1D, Double> function,
      Function1D<DoubleMatrix1D, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(domain, "domain");

    double[] wFwd = new double[] {-3. / twoEps, 4. / twoEps, -1. / twoEps};
    double[] wCent = new double[] {-1. / twoEps, 0., 1. / twoEps};
    double[] wBack = new double[] {1. / twoEps, -4. / twoEps, 3. / twoEps};

    return new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
        ArgChecker.notNull(x, "x");
        ArgChecker.isTrue(domain.evaluate(x), "point {} is not in the function domain", x.toString());

        int n = x.getNumberOfElements();
        double[] xData = x.getData();
        double oldValue;
        double[] y = new double[3];
        double[] res = new double[n];
        double[] w;
        for (int i = 0; i < n; i++) {
          oldValue = xData[i];
          xData[i] += eps;
          if (!domain.evaluate(x)) {
            xData[i] = oldValue - twoEps;
            if (!domain.evaluate(x)) {
              throw new MathException("cannot get derivative at point " + x.toString() + " in direction " + i);
            }
            y[0] = function.evaluate(x);
            xData[i] = oldValue;
            y[2] = function.evaluate(x);
            xData[i] = oldValue - eps;
            y[1] = function.evaluate(x);
            w = wBack;
          } else {
            double temp = function.evaluate(x);
            xData[i] = oldValue - eps;
            if (!domain.evaluate(x)) {
              y[1] = temp;
              xData[i] = oldValue;
              y[0] = function.evaluate(x);
              xData[i] = oldValue + twoEps;
              y[2] = function.evaluate(x);
              w = wFwd;
            } else {
              y[2] = temp;
              xData[i] = oldValue - eps;
              y[0] = function.evaluate(x);
              w = wCent;
            }
          }
          res[i] = y[0] * w[0] + y[2] * w[2];
          if (w[1] != 0) {
            res[i] += y[1] * w[1];
          }
          xData[i] = oldValue;
        }
        return new DoubleMatrix1D(res);
      }
    };
  }

}
