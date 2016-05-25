/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Differentiates a scalar function with respect to its argument using finite difference.
 * <p>
 * For a function $y = f(x)$ where $x$ and $y$ are scalars, this class produces
 * a function that returns the second derivative value for each point, i.e., $\frac{d^2 f}{dx^2}$.
 */
public class ScalarSecondOrderDifferentiator
    implements Differentiator<Double, Double, Double> {

  /**
   * Default steps size.
   */
  private static final double DEFAULT_EPS = 1e-4;

  private final double eps;
  private final double epsSqr;
  private final double twoEps;
  private final double threeEps;

  /**
   * Creates an instance using the default values.
   */
  public ScalarSecondOrderDifferentiator() {
    this(DEFAULT_EPS);
  }

  /**
   * Creates an instance specifying the step size. 
   * 
   * @param eps  the step size
   */
  public ScalarSecondOrderDifferentiator(double eps) {
    this.eps = eps;
    this.epsSqr = eps * eps;
    this.twoEps = 2d * eps;
    this.threeEps = 3d * eps;
  }

  //-------------------------------------------------------------------------
  @Override
  public Function<Double, Double> differentiate(Function<Double, Double> function) {
    ArgChecker.notNull(function, "function");
    return new Function<Double, Double>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public Double apply(Double x) {
        ArgChecker.notNull(x, "x");
        return (function.apply(x + eps) + function.apply(x - eps) - 2d * function.apply(x)) / epsSqr;
      }
    };
  }

  //-------------------------------------------------------------------------
  @Override
  public Function<Double, Double> differentiate(Function<Double, Double> function, Function<Double, Boolean> domain) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(domain, "domain");
    return new Function<Double, Double>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public Double apply(Double x) {
        ArgChecker.notNull(x, "x");
        ArgChecker.isTrue(domain.apply(x), "point {} is not in the function domain", x.toString());
        if (!domain.apply(x + threeEps)) {
          if (!domain.apply(x - threeEps)) {
            throw new IllegalArgumentException("cannot get derivative at point " + x.toString());
          }
          return (-function.apply(x - threeEps) + 4d * function.apply(x - twoEps)
              - 5d * function.apply(x - eps) + 2d * function.apply(x)) / epsSqr;
        } else {
          if (!domain.apply(x - eps)) {
            return (-function.apply(x + threeEps) + 4d * function.apply(x + twoEps)
                - 5d * function.apply(x + eps) + 2d * function.apply(x)) / epsSqr;
          } 
          return (function.apply(x + eps) + function.apply(x - eps) - 2d * function.apply(x)) / epsSqr;
        }
      }
    };
  }
}
