/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import java.util.function.Function;

import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.special.Gamma;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;

/**
 * The incomplete gamma function is defined as:
 * $$
 * \begin{equation*}
 * P(a, x) = \frac{\gamma(a, x)}{\Gamma(a)}\int_0^x e^{-t}t^{a-1}dt
 * \end{equation*}
 * $$
 * where $a > 0$.
 * <p>
 * This class is a wrapper for the Commons Math library implementation of the incomplete gamma
 * function <a href="http://commons.apache.org/math/api-2.1/index.html">link</a>
 */
public class IncompleteGammaFunction implements Function<Double, Double> {

  private final int _maxIter;
  private final double _eps;
  private final double _a;

  public IncompleteGammaFunction(double a) {
    ArgChecker.notNegativeOrZero(a, "a");
    _maxIter = 100000;
    _eps = 1e-12;
    _a = a;
  }

  public IncompleteGammaFunction(double a, int maxIter, double eps) {
    ArgChecker.notNegativeOrZero(a, "a");
    ArgChecker.notNegative(eps, "eps");
    if (maxIter < 1) {
      throw new IllegalArgumentException("Must have at least one iteration");
    }
    _maxIter = maxIter;
    _eps = eps;
    _a = a;
  }

  //-------------------------------------------------------------------------
  @Override
  public Double apply(Double x) {
    try {
      return Gamma.regularizedGammaP(_a, x, _eps, _maxIter);
    } catch (MaxCountExceededException e) {
      throw new MathException(e);
    }
  }

}
