/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * 
 */
public class OrthonormalHermitePolynomialFunction extends OrthogonalPolynomialFunctionGenerator {

  private static final double C1 = 1. / Math.pow(Math.PI, 0.25);
  private static final double C2 = Math.sqrt(2) * C1;
  private static final RealPolynomialFunction1D F0 = new RealPolynomialFunction1D(new double[] {C1});
  private static final RealPolynomialFunction1D DF1 = new RealPolynomialFunction1D(new double[] {C2});

  @Override
  public DoubleFunction1D[] getPolynomials(int n) {
    ArgChecker.isTrue(n >= 0);
    DoubleFunction1D[] polynomials = new DoubleFunction1D[n + 1];
    for (int i = 0; i <= n; i++) {
      if (i == 0) {
        polynomials[i] = F0;
      } else if (i == 1) {
        polynomials[i] = polynomials[0].multiply(Math.sqrt(2)).multiply(getX());
      } else {
        polynomials[i] =
            polynomials[i - 1].multiply(getX()).multiply(Math.sqrt(2. / i))
                .subtract(polynomials[i - 2].multiply(Math.sqrt((i - 1.) / i)));
      }
    }
    return polynomials;
  }

  @Override
  public Pair<DoubleFunction1D, DoubleFunction1D>[] getPolynomialsAndFirstDerivative(int n) {
    ArgChecker.isTrue(n >= 0);
    @SuppressWarnings("unchecked")
    Pair<DoubleFunction1D, DoubleFunction1D>[] polynomials = new Pair[n + 1];
    DoubleFunction1D p, dp, p1, p2;
    double sqrt2 = Math.sqrt(2);
    DoubleFunction1D x = getX();
    for (int i = 0; i <= n; i++) {
      if (i == 0) {
        polynomials[i] = Pair.of((DoubleFunction1D) F0, getZero());
      } else if (i == 1) {
        polynomials[i] = Pair.of(polynomials[0].getFirst().multiply(sqrt2).multiply(x), (DoubleFunction1D) DF1);
      } else {
        p1 = polynomials[i - 1].getFirst();
        p2 = polynomials[i - 2].getFirst();
        p = p1.multiply(x).multiply(Math.sqrt(2. / i)).subtract(p2.multiply(Math.sqrt((i - 1.) / i)));
        dp = p1.multiply(Math.sqrt(2 * i));
        polynomials[i] = Pair.of(p, dp);
      }
    }
    return polynomials;
  }

}
