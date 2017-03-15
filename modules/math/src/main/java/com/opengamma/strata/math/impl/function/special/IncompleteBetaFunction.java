/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import java.util.function.Function;

import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.special.Beta;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;

/**
 * The incomplete beta function is defined as:
 * $$
 * \begin{equation*}
 * I_x(a, b)=\frac{B_x(a, b)}{B(a, b)}\int_0^x t^{a-1}(1-t)^{b-1}dt
 * \end{equation*}
 * $$
 * where $a,b>0$.
 * <p>
 * This class uses the <a href="http://commons.apache.org/math/api-2.1/org/apache/commons/math/special/Beta.html">Commons Math library implementation</a> of the Beta function.
 */
public class IncompleteBetaFunction implements Function<Double, Double> {

  private final double _a;
  private final double _b;
  private final double _eps;
  private final int _maxIter;

  /**
   * Creates an instance using the default values for the accuracy
   * ({@code 10^-12}) and number of iterations ({@code 10000}).
   * 
   * @param a  a, $a > 0$
   * @param b  b, $b > 0$
   */
  public IncompleteBetaFunction(double a, double b) {
    this(a, b, 1e-12, 10000);
  }

  /**
   * Creates an instance.
   * 
   * @param a  a, $a > 0$
   * @param b  b, $b > 0$
   * @param eps  approximation accuracy, $\epsilon \geq 0$
   * @param maxIter  maximum number of iterations, $\iter \geq 1$
   */
  public IncompleteBetaFunction(double a, double b, double eps, int maxIter) {
    ArgChecker.isTrue(a > 0, "a must be > 0");
    ArgChecker.isTrue(b > 0, "b must be > 0");
    ArgChecker.isTrue(eps >= 0, "eps must not be negative");
    ArgChecker.isTrue(maxIter >= 1, "maximum number of iterations must be greater than zero");
    _a = a;
    _b = b;
    _eps = eps;
    _maxIter = maxIter;
  }

  //-------------------------------------------------------------------------
  /**
   * Evaluates the function.
   * 
   * @param x  x
   * @return the value of the function
   * @throws IllegalArgumentException if $x < 0$ or $x > 1$
   */
  @Override
  public Double apply(Double x) {
    ArgChecker.isTrue(x >= 0 && x <= 1, "x must be in the range 0 to 1");
    try {
      return Beta.regularizedBeta(x, _a, _b, _eps, _maxIter);
    } catch (MaxCountExceededException e) {
      throw new MathException(e);
    }
  }

}
