/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import java.util.function.Function;

import com.opengamma.strata.math.MathException;

/**
 * Root finder.
 */
public class BrentSingleRootFinder extends RealSingleRootFinder {

  private static final int MAX_ITER = 100;
  private static final double ZERO = 1e-16;
  private final double _accuracy;

  /**
   * Creates an instance.
   * Sets the accuracy to 10<sup>-15</sup>
   */
  public BrentSingleRootFinder() {
    this(1e-15);
  }

  /**
   * Creates an instance.
   * @param accuracy The accuracy of the root
   */
  public BrentSingleRootFinder(double accuracy) {
    _accuracy = accuracy;
  }

  //-------------------------------------------------------------------------
  @Override
  public Double getRoot(Function<Double, Double> function, Double xLower, Double xUpper) {
    checkInputs(function, xLower, xUpper);
    if (xLower.equals(xUpper)) {
      return xLower;
    }
    double x1 = xLower;
    double x2 = xUpper;
    double x3 = xUpper;
    double delta = 0;
    double oldDelta = 0;
    double f1 = function.apply(x1);
    double f2 = function.apply(x2);
    double f3 = f2;
    double r1, r2, r3, r4, eps, xMid, min1, min2;
    for (int i = 0; i < MAX_ITER; i++) {
      if (f2 > 0 && f3 > 0 || f2 < 0 && f3 < 0) {
        x3 = x1;
        f3 = f1;
        delta = x2 - x1;
        oldDelta = delta;
      }
      if (Math.abs(f3) < Math.abs(f2)) {
        x1 = x2;
        x2 = x3;
        x3 = x1;
        f1 = f2;
        f2 = f3;
        f3 = f1;
      }
      eps = 2 * ZERO * Math.abs(x2) + 0.5 * _accuracy;
      xMid = (x3 - x2) / 2;
      if (Math.abs(xMid) <= eps) {
        return x2;
      }
      if (Math.abs(oldDelta) >= eps && Math.abs(f1) > Math.abs(f2)) {
        r4 = f2 / f1;
        if (Math.abs(x1 - x3) < ZERO) {
          r1 = 2 * xMid * r4;
          r2 = 1 - r4;
        } else {
          r2 = f1 / f3;
          r3 = f2 / f3;
          r1 = r4 * (2 * xMid * r2 * (r2 - r3) - (x2 - x1) * (r3 - 1));
          r2 = (r2 - 1) * (r3 - 1) * (r4 - 1);
        }
        if (r1 > 0) {
          r2 *= -1;
        }
        r1 = Math.abs(r1);
        min1 = 3 * xMid * r2 - Math.abs(eps * r2);
        min2 = Math.abs(oldDelta * r2);
        if (2 * r1 < (min1 < min2 ? min1 : min2)) {
          oldDelta = delta;
          delta = r1 / r2;
        } else {
          delta = xMid;
          oldDelta = delta;
        }
      } else {
        delta = xMid;
        oldDelta = delta;
      }
      x1 = x2;
      if (Math.abs(delta) > eps) {
        x2 += delta;
      } else {
        x2 += Math.copySign(eps, xMid);
      }
      f1 = function.apply(x1);
      f2 = function.apply(x2);
      f3 = function.apply(x3);
    }
    throw new MathException("Could not converge to root in " + MAX_ITER + " attempts");
  }

}
