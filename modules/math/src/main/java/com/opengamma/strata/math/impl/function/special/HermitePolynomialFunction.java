/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;

/**
 * 
 */
public class HermitePolynomialFunction extends OrthogonalPolynomialFunctionGenerator {

  private static final DoubleFunction1D TWO_X = x -> 2 * x;

  @Override
  public DoubleFunction1D[] getPolynomials(int n) {
    ArgChecker.isTrue(n >= 0);
    DoubleFunction1D[] polynomials = new DoubleFunction1D[n + 1];
    for (int i = 0; i <= n; i++) {
      if (i == 0) {
        polynomials[i] = getOne();
      } else if (i == 1) {
        polynomials[i] = TWO_X;
      } else {
        polynomials[i] = polynomials[i - 1]
            .multiply(2)
            .multiply(getX())
            .subtract(polynomials[i - 2].multiply(2 * i - 2));
      }
    }
    return polynomials;
  }

  @Override
  public Pair<DoubleFunction1D, DoubleFunction1D>[] getPolynomialsAndFirstDerivative(int n) {
    throw new UnsupportedOperationException();
  }

}
