/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * 
 */
//CSOFF: JavadocMethod
public abstract class OrthogonalPolynomialFunctionGenerator {

  private static final RealPolynomialFunction1D ZERO = new RealPolynomialFunction1D(new double[] {0});
  private static final RealPolynomialFunction1D ONE = new RealPolynomialFunction1D(new double[] {1});
  private static final RealPolynomialFunction1D X = new RealPolynomialFunction1D(new double[] {0, 1});

  public abstract DoubleFunction1D[] getPolynomials(int n);

  public abstract Pair<DoubleFunction1D, DoubleFunction1D>[] getPolynomialsAndFirstDerivative(int n);

  protected DoubleFunction1D getZero() {
    return ZERO;
  }

  protected DoubleFunction1D getOne() {
    return ONE;
  }

  protected DoubleFunction1D getX() {
    return X;
  }

}
