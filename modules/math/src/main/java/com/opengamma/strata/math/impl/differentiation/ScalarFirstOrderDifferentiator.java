/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Differentiates a scalar function with respect to its argument using finite difference.
 * <p>
 * For a function $y = f(x)$ where $x$ and $y$ are scalars, this class produces
 * a gradient function $g(x)$, i.e. a function that returns the gradient for
 * each point $x$, where $g$ is the scalar $\frac{dy}{dx}$.
 */
public class ScalarFirstOrderDifferentiator
    implements Differentiator<Double, Double, Double> {

  private static final double DEFAULT_EPS = 1e-5;
  private static final double MIN_EPS = Math.sqrt(Double.MIN_NORMAL);

  private final double eps;
  private final double twoEps;
  private final FiniteDifferenceType differenceType;

  /**
   * Creates an instance using the default value of eps (10<sup>-5</sup>) and central differencing type.
   */
  public ScalarFirstOrderDifferentiator() {
    this(FiniteDifferenceType.CENTRAL, DEFAULT_EPS);
  }

  /**
   * Creates an instance using the default value of eps (10<sup>-5</sup>).
   * 
   * @param differenceType  the differencing type to be used in calculating the gradient function
   */
  public ScalarFirstOrderDifferentiator(FiniteDifferenceType differenceType) {
    this(differenceType, DEFAULT_EPS);
  }

  /**
   * Creates an instance.
   * <p>
   * If the size of the domain is very small or very large, consider re-scaling first.
   * If this value is too small, the result will most likely be dominated by noise.
   * Use around 10<sup>-5</sup> times the domain size.
   * 
   * @param differenceType  the differencing type to be used in calculating the gradient function
   * @param eps  the step size used to approximate the derivative
   */
  public ScalarFirstOrderDifferentiator(FiniteDifferenceType differenceType, double eps) {
    ArgChecker.notNull(differenceType, "differenceType");
    ArgChecker.isTrue(eps >= MIN_EPS,
        "eps of {} is too small. Please choose a value > {}, such as 1e-5*size of domain", eps, MIN_EPS);
    this.differenceType = differenceType;
    this.eps = eps;
    this.twoEps = 2 * eps;
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<Double, Double> differentiate(Function1D<Double, Double> function) {
    ArgChecker.notNull(function, "function");
    switch (differenceType) {
      case FORWARD:
        return new Function1D<Double, Double>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public Double evaluate(Double x) {
            ArgChecker.notNull(x, "x");
            return (function.evaluate(x + eps) - function.evaluate(x)) / eps;
          }
        };
      case CENTRAL:
        return new Function1D<Double, Double>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public Double evaluate(Double x) {
            ArgChecker.notNull(x, "x");
            return (function.evaluate(x + eps) - function.evaluate(x - eps)) / twoEps;
          }
        };
      case BACKWARD:
        return new Function1D<Double, Double>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public Double evaluate(Double x) {
            ArgChecker.notNull(x, "x");
            return (function.evaluate(x) - function.evaluate(x - eps)) / eps;
          }
        };
      default:
        throw new IllegalArgumentException("Can only handle forward, backward and central differencing");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<Double, Double> differentiate(
      Function1D<Double, Double> function,
      Function1D<Double, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(domain, "domain");
    double[] wFwd = new double[] {-3. / twoEps, 4. / twoEps, -1. / twoEps};
    double[] wCent = new double[] {-1. / twoEps, 0., 1. / twoEps};
    double[] wBack = new double[] {1. / twoEps, -4. / twoEps, 3. / twoEps};

    return new Function1D<Double, Double>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public Double evaluate(Double x) {
        ArgChecker.notNull(x, "x");
        ArgChecker.isTrue(domain.evaluate(x), "point {} is not in the function domain", x.toString());

        double[] y = new double[3];
        double[] w;

        if (!domain.evaluate(x + eps)) {
          if (!domain.evaluate(x - eps)) {
            throw new MathException("cannot get derivative at point " + x.toString());
          }
          y[0] = function.evaluate(x - twoEps);
          y[1] = function.evaluate(x - eps);
          y[2] = function.evaluate(x);
          w = wBack;
        } else {
          if (!domain.evaluate(x - eps)) {
            y[0] = function.evaluate(x);
            y[1] = function.evaluate(x + eps);
            y[2] = function.evaluate(x + twoEps);
            w = wFwd;
          } else {
            y[0] = function.evaluate(x - eps);
            y[2] = function.evaluate(x + eps);
            w = wCent;
          }
        }

        double res = y[0] * w[0] + y[2] * w[2];
        if (w[1] != 0) {
          res += y[1] * w[1];
        }
        return res;
      }
    };
  }

}
