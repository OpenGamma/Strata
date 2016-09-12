/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.ComplexNumber;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Class that calculates the roots of a cubic equation. 
 * <p>
 * As the polynomial has real coefficients, the roots of the cubic can be found using the method described
 * <a href="http://mathworld.wolfram.com/CubicFormula.html">here</a>.
 */
public class CubicRootFinder implements Polynomial1DRootFinder<ComplexNumber> {

  private static final double TWO_PI = 2 * Math.PI;

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If the function is not cubic
   */
  @Override
  public ComplexNumber[] getRoots(RealPolynomialFunction1D function) {
    ArgChecker.notNull(function, "function");
    double[] coefficients = function.getCoefficients();
    ArgChecker.isTrue(coefficients.length == 4, "Function is not a cubic");
    double divisor = coefficients[3];
    double a = coefficients[2] / divisor;
    double b = coefficients[1] / divisor;
    double c = coefficients[0] / divisor;
    double aSq = a * a;
    double q = (aSq - 3 * b) / 9;
    double r = (2 * a * aSq - 9 * a * b + 27 * c) / 54;
    double rSq = r * r;
    double qCb = q * q * q;
    double constant = a / 3;
    if (rSq < qCb) {
      double mult = -2 * Math.sqrt(q);
      double theta = Math.acos(r / Math.sqrt(qCb));
      return new ComplexNumber[] {
          new ComplexNumber(mult * Math.cos(theta / 3) - constant, 0),
          new ComplexNumber(mult * Math.cos((theta + TWO_PI) / 3) - constant, 0),
          new ComplexNumber(mult * Math.cos((theta - TWO_PI) / 3) - constant, 0)};
    }
    double s = -Math.signum(r) * Math.cbrt(Math.abs(r) + Math.sqrt(rSq - qCb));
    double t = DoubleMath.fuzzyEquals(s, 0d, 1e-16) ? 0 : q / s;
    double sum = s + t;
    double real = -0.5 * sum - constant;
    double imaginary = Math.sqrt(3) * (s - t) / 2;
    return new ComplexNumber[] {
        new ComplexNumber(sum - constant, 0),
        new ComplexNumber(real, imaginary),
        new ComplexNumber(real, -imaginary)};
  }

}
