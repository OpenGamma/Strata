/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.MathException;

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
    implements Differentiator<DoubleArray, Double, DoubleArray> {

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
  public Function<DoubleArray, DoubleArray> differentiate(
      Function<DoubleArray, Double> function) {

    ArgChecker.notNull(function, "function");
    switch (differenceType) {
      case FORWARD:
        return new Function<DoubleArray, DoubleArray>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleArray apply(DoubleArray x) {
            ArgChecker.notNull(x, "x");
            double y = function.apply(x);
            return DoubleArray.of(x.size(), i -> {
              double up = function.apply(x.with(i, x.get(i) + eps));
              return (up - y) / eps;
            });
          }
        };
      case CENTRAL:
        return new Function<DoubleArray, DoubleArray>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleArray apply(DoubleArray x) {
            ArgChecker.notNull(x, "x");
            return DoubleArray.of(x.size(), i -> {
              double up = function.apply(x.with(i, x.get(i) + eps));
              double down = function.apply(x.with(i, x.get(i) - eps));
              return (up - down) / twoEps;
            });
          }
        };
      case BACKWARD:
        return new Function<DoubleArray, DoubleArray>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleArray apply(DoubleArray x) {
            ArgChecker.notNull(x, "x");
            double y = function.apply(x);
            return DoubleArray.of(x.size(), i -> {
              double down = function.apply(x.with(i, x.get(i) - eps));
              return (y - down) / eps;
            });
          }
        };
      default:
        throw new IllegalArgumentException("Can only handle forward, backward and central differencing");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Function<DoubleArray, DoubleArray> differentiate(
      Function<DoubleArray, Double> function,
      Function<DoubleArray, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(domain, "domain");

    double[] wFwd = new double[] {-3. / twoEps, 4. / twoEps, -1. / twoEps};
    double[] wCent = new double[] {-1. / twoEps, 0., 1. / twoEps};
    double[] wBack = new double[] {1. / twoEps, -4. / twoEps, 3. / twoEps};

    return new Function<DoubleArray, DoubleArray>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleArray apply(DoubleArray x) {
        ArgChecker.notNull(x, "x");
        ArgChecker.isTrue(domain.apply(x), "point {} is not in the function domain", x.toString());

        return DoubleArray.of(x.size(), i -> {
          double xi = x.get(i);
          DoubleArray xPlusOneEps = x.with(i, xi + eps);
          DoubleArray xMinusOneEps = x.with(i, xi - eps);
          double y0, y1, y2;
          double[] w;
          if (!domain.apply(xPlusOneEps)) {
            DoubleArray xMinusTwoEps = x.with(i, xi - twoEps);
            if (!domain.apply(xMinusTwoEps)) {
              throw new MathException("cannot get derivative at point " + x.toString() + " in direction " + i);
            }
            y0 = function.apply(xMinusTwoEps);
            y2 = function.apply(x);
            y1 = function.apply(xMinusOneEps);
            w = wBack;
          } else {
            double temp = function.apply(xPlusOneEps);
            if (!domain.apply(xMinusOneEps)) {
              y1 = temp;
              y0 = function.apply(x);
              y2 = function.apply(x.with(i, xi + twoEps));
              w = wFwd;
            } else {
              y1 = 0;
              y2 = temp;
              y0 = function.apply(xMinusOneEps);
              w = wCent;
            }
          }
          double res = y0 * w[0] + y2 * w[2];
          if (w[1] != 0) {
            res += y1 * w[1];
          }
          return res;
        });
      }
    };
  }

}
