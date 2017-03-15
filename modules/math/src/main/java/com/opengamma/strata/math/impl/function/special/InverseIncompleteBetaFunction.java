/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import java.util.function.Function;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;

/**
 * 
 */
public class InverseIncompleteBetaFunction implements Function<Double, Double> {
//TODO either find another implementation or delete this class

  private final double _a;
  private final double _b;
  private final Function<Double, Double> _lnGamma = new NaturalLogGammaFunction();
  private final Function<Double, Double> _beta;
  private static final double EPS = 1e-9;

  /**
   * Creates an instance.
   * 
   * @param a  the a value
   * @param b  the b value
   */
  public InverseIncompleteBetaFunction(double a, double b) {
    ArgChecker.notNegativeOrZero(a, "a");
    ArgChecker.notNegativeOrZero(b, "b");
    _a = a;
    _b = b;
    _beta = new IncompleteBetaFunction(a, b);
  }

  //-------------------------------------------------------------------------
  @Override
  public Double apply(Double x) {
    ArgChecker.inRangeInclusive(x, 0d, 1d, "x");
    double pp, p, t, h, w, lnA, lnB, u, a1 = _a - 1;
    double b1 = _b - 1;
    if (_a >= 1 && _b >= 1) {
      pp = x < 0.5 ? x : 1 - x;
      t = Math.sqrt(-2 * Math.log(pp));
      p = (2.30753 + t * 0.27061) / (1 + t * (0.99229 + t * 0.04481)) - t;
      if (p < 0.5) {
        p *= -1;
      }
      a1 = (Math.sqrt(p) - 3.) / 6.;
      double tempA = 1. / (2 * _a - 1);
      double tempB = 1. / (2 * _b - 1);
      h = 2. / (tempA + tempB);
      w = p * Math.sqrt(a1 + h) / h - (tempB - tempA) * (a1 + 5. / 6 - 2. / (3 * h));
      p = _a / (_a + _b + Math.exp(2 * w));
    } else {
      lnA = Math.log(_a / (_a + _b));
      lnB = Math.log(_b / (_a + _b));
      t = Math.exp(_a * lnA) / _a;
      u = Math.exp(_b * lnB) / _b;
      w = t + u;
      if (x < t / w) {
        p = Math.pow(_a * w * x, 1. / _a);
      } else {
        p = 1 - Math.pow(_b * w * (1 - x), 1. / _b);
      }
    }
    double afac = -_lnGamma.apply(_a) - _lnGamma.apply(_b) + _lnGamma.apply(_a + _b);
    double error;
    for (int j = 0; j < 10; j++) {
      if (DoubleMath.fuzzyEquals(p, 0d, 1e-16) || DoubleMath.fuzzyEquals(p, (double) 1, 1e-16)) {
        throw new MathException("a or b too small for accurate evaluation");
      }
      error = _beta.apply(p) - x;
      t = Math.exp(a1 * Math.log(p) + b1 * Math.log(1 - p) + afac);
      u = error / t;
      t = u / (1 - 0.5 * Math.min(1, u * (a1 / p - b1 / (1 - p))));
      p -= t;
      if (p <= 0) {
        p = 0.5 * (p + t);
      }
      if (p >= 1) {
        p = 0.5 * (p + t + 1);
      }
      if (Math.abs(t) < EPS * p && j > 0) {
        break;
      }
    }
    return p;
  }

}
