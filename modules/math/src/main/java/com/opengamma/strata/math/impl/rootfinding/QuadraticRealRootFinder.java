/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Class that calculates the real roots of a quadratic function. 
 * <p>
 * The roots can be found analytically. For a quadratic $ax^2 + bx + c = 0$, the roots are given by:
 * $$
 * \begin{align*}
 * x_{1, 2} = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
 * \end{align*}
 * $$
 * If no real roots exist (i.e. $b^2 - 4ac < 0$) then an exception is thrown.
 */
public class QuadraticRealRootFinder implements Polynomial1DRootFinder<Double> {

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If the function is not a quadratic
   * @throws MathException If the roots are not real
   */
  @Override
  public Double[] getRoots(final RealPolynomialFunction1D function) {
    ArgChecker.notNull(function, "function");
    final double[] coefficients = function.getCoefficients();
    ArgChecker.isTrue(coefficients.length == 3, "Function is not a quadratic");
    final double c = coefficients[0];
    final double b = coefficients[1];
    final double a = coefficients[2];
    final double discriminant = b * b - 4 * a * c;
    if (discriminant < 0) {
      throw new MathException("No real roots for quadratic");
    }
    final double q = -0.5 * (b + Math.signum(b) * discriminant);
    return new Double[] {q / a, c / q };
  }
}
