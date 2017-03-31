/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;

/**
 * Class for finding the real root of a function within a range of $x$-values using the one-dimensional version of Newton's method.
 * <p>
 * For a function $f(x)$, the Taylor series expansion is given by:
 * $$
 * \begin{align*}
 * f(x + \delta) \approx f(x) + f'(x)\delta + \frac{f''(x)}{2}\delta^2 + \cdots
 * \end{align*}
 * $$
 * As delta approaches zero (and if the function is well-behaved), this gives 
 * $$
 * \begin{align*}
 * \delta = -\frac{f(x)}{f'(x)}
 * \end{align*}
 * $$
 * when $f(x + \delta) = 0$.
 * <p>
 * There are several well-known problems with Newton's method, in particular when the range of values given includes a local
 * maximum or minimum. In this situation, the next iterative step can shoot off to $\pm\infty$. This implementation
 * currently does not attempt to correct for this: if the value of $x$ goes beyond the initial range of values $x_{low}$
 * and $x_{high}$, an exception is thrown.
 * <p>
 * If the function that is provided does not override the {@link com.opengamma.strata.math.impl.function.DoubleFunction1D#derivative()} method, then 
 * the derivative is approximated using finite difference. This is undesirable for several reasons: (i) the extra function evaluations will lead
 * to slower convergence; and (ii) the choice of shift size is very important (too small and the result will be dominated by rounding errors, too large
 * and convergence will be even slower). Use of another root-finder is recommended in this case.
 */
// CSOFF: JavadocMethod
public class NewtonRaphsonSingleRootFinder extends RealSingleRootFinder {

  private static final int MAX_ITER = 10000;
  private final double _accuracy;

  /**
   * Default constructor. Sets accuracy to 1e-12.
   */
  public NewtonRaphsonSingleRootFinder() {
    this(1e-12);
  }

  /**
   * Takes the accuracy of the root as a parameter - this is the maximum difference
   * between the true root and the returned value that is allowed. 
   * If this is negative, then the absolute value is used.
   * 
   * @param accuracy The accuracy
   */
  public NewtonRaphsonSingleRootFinder(double accuracy) {
    _accuracy = Math.abs(accuracy);
  }

  /**
   * {@inheritDoc}
   * @throws MathException If the root is not found in 1000 attempts; if the Newton
   *   step takes the estimate for the root outside the original bounds.
   */
  @Override
  public Double getRoot(Function<Double, Double> function, Double x1, Double x2) {
    ArgChecker.notNull(function, "function");
    return getRoot(DoubleFunction1D.from(function), x1, x2);
  }

  //-------------------------------------------------------------------------
  public Double getRoot(Function<Double, Double> function, Double x) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(x, "x");
    DoubleFunction1D f = DoubleFunction1D.from(function);
    return getRoot(f, f.derivative(), x);
  }

  /**
   * Uses the {@link DoubleFunction1D#derivative()} method. <i>x<sub>1</sub></i> and
   * <i>x<sub>2</sub></i> do not have to be increasing.
   * 
   * @param function The function, not null
   * @param x1 The first bound of the root, not null
   * @param x2 The second bound of the root, not null
   * @return The root
   * @throws MathException If the root is not found in 1000 attempts; if the Newton
   *   step takes the estimate for the root outside the original bounds.
   */
  public Double getRoot(DoubleFunction1D function, Double x1, Double x2) {
    ArgChecker.notNull(function, "function");
    return getRoot(function, function.derivative(), x1, x2);
  }

  /**
   * Uses the {@link DoubleFunction1D#derivative()} method. This method uses an initial
   * guess for the root, rather than bounds.
   * 
   * @param function The function, not null
   * @param x The initial guess for the root, not null
   * @return The root
   * @throws MathException If the root is not found in 1000 attempts.
   */
  public Double getRoot(DoubleFunction1D function, Double x) {
    ArgChecker.notNull(function, "function");
    return getRoot(function, function.derivative(), x);
  }

  /**
   * Uses the function and its derivative. 
   * @param function The function, not null
   * @param derivative The derivative, not null
   * @param x1 The first bound of the root, not null
   * @param x2 The second bound of the root, not null
   * @return The root
   * @throws MathException If the root is not found in 1000 attempts; if the Newton
   *   step takes the estimate for the root outside the original bounds.
   */
  public Double getRoot(Function<Double, Double> function, Function<Double, Double> derivative, Double x1, Double x2) {
    checkInputs(function, x1, x2);
    ArgChecker.notNull(derivative, "derivative");
    return getRoot(DoubleFunction1D.from(function), DoubleFunction1D.from(derivative), x1, x2);
  }

  /**
   * Uses the function and its derivative. This method uses an initial guess for the root, rather than bounds.
   * @param function The function, not null
   * @param derivative The derivative, not null
   * @param x The initial guess for the root, not null
   * @return The root
   * @throws MathException If the root is not found in 1000 attempts.
   */
  public Double getRoot(Function<Double, Double> function, Function<Double, Double> derivative, Double x) {
    return getRoot(DoubleFunction1D.from(function), DoubleFunction1D.from(derivative), x);
  }

  /**
   * Uses the function and its derivative.
   * @param function The function, not null
   * @param derivative The derivative, not null
   * @param x1 The first bound of the root, not null
   * @param x2 The second bound of the root, not null
   * @return The root
   * @throws MathException If the root is not found in 1000 attempts; if the Newton
   *   step takes the estimate for the root outside the original bounds.
   */
  public Double getRoot(DoubleFunction1D function, DoubleFunction1D derivative, Double x1, Double x2) {
    checkInputs(function, x1, x2);
    ArgChecker.notNull(derivative, "derivative function");
    double y1 = function.applyAsDouble(x1);
    if (Math.abs(y1) < _accuracy) {
      return x1;
    }
    double y2 = function.applyAsDouble(x2);
    if (Math.abs(y2) < _accuracy) {
      return x2;
    }
    double x = (x1 + x2) / 2;
    double x3 = y2 < 0 ? x2 : x1;
    double x4 = y2 < 0 ? x1 : x2;
    double xLower = x1 > x2 ? x2 : x1;
    double xUpper = x1 > x2 ? x1 : x2;
    for (int i = 0; i < MAX_ITER; i++) {
      double y = function.applyAsDouble(x);
      double dy = derivative.applyAsDouble(x);
      double dx = -y / dy;
      if (Math.abs(dx) <= _accuracy) {
        return x + dx;
      }
      x += dx;
      if (x < xLower || x > xUpper) {
        dx = (x4 - x3) / 2;
        x = x3 + dx;
      }
      if (y < 0) {
        x3 = x;
      } else {
        x4 = x;
      }
    }
    throw new MathException("Could not find root in " + MAX_ITER + " attempts");
  }

  /**
   * Uses the function and its derivative. This method uses an initial guess for the root, rather than bounds.
   * @param function The function, not null
   * @param derivative The derivative, not null
   * @param x The initial guess for the root, not null
   * @return The root
   * @throws MathException If the root is not found in 1000 attempts.
   */
  public Double getRoot(DoubleFunction1D function, DoubleFunction1D derivative, Double x) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(derivative, "derivative function");
    ArgChecker.notNull(x, "x");
    double root = x;
    for (int i = 0; i < MAX_ITER; i++) {
      double y = function.applyAsDouble(root);
      double dy = derivative.applyAsDouble(root);
      double dx = y / dy;
      if (Math.abs(dx) <= _accuracy) {
        return root - dx;
      }
      root -= dx;
    }
    throw new MathException("Could not find root in " + MAX_ITER + " attempts");
  }

}
