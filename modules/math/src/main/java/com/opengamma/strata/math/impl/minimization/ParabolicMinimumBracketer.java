/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import java.util.function.Function;

import com.opengamma.strata.math.MathException;

/**
 * 
 */
public class ParabolicMinimumBracketer extends MinimumBracketer {

  private static final double ZERO = 1e-20;
  private static final int MAX_ITER = 100;
  private static final int MAX_MAGNIFICATION = 100;
  private static final double MAGNIFICATION = 1 + GOLDEN;

  @Override
  public double[] getBracketedPoints(Function<Double, Double> f, double xLower, double xUpper) {
    checkInputs(f, xLower, xUpper);
    double temp;
    double x1 = xLower;
    double x2 = xUpper;
    double f1 = f.apply(x1);
    double f2 = f.apply(x2);
    if (f2 > f1) {
      temp = x2;
      x2 = x1;
      x1 = temp;
      temp = f2;
      f2 = f1;
      f1 = temp;
    }
    double x3 = x2 + MAGNIFICATION * (x2 - x1);
    double f3 = f.apply(x3);
    if (x1 < x2 && x2 < x3 && f2 < f1 && f2 < f3 || x1 > x2 && x2 > x3 && f2 < f1 && f2 < f3) {
      return new double[] {x1, x2, x3};
    }
    double r, q, u, uLim, fu;
    int count = 0;
    while (count < MAX_ITER) {
      if (f2 < f3) {
        return new double[] {x1, x2, x3};
      }
      count++;
      r = (x2 - x1) * (f2 - f3);
      q = (x2 - x1) * (f2 - f1);
      u = x2 - ((x2 - x3) * q - (x2 - x1) * r) / (2 * Math.copySign(Math.max(Math.abs(q - r), ZERO), q - r));
      uLim = x2 + MAX_MAGNIFICATION * (x3 - x2);
      if ((x2 - u) * (u - x3) > 0) {
        fu = f.apply(u);
        if (fu < f3) {
          x1 = x2;
          x2 = u;
          return new double[] {x1, x2, x3};
        } else if (fu > f2) {
          x3 = u;
          return new double[] {x1, x2, x3};
        }
        u = x3 + MAGNIFICATION * (x3 - x2);
        fu = f.apply(u);
      } else if ((x3 - u) * (u - uLim) > 0) {
        fu = f.apply(u);
        if (fu < f3) {
          temp = u + MAGNIFICATION * (u - x3);
          x2 = x3;
          x3 = u;
          u = temp;
          f2 = f3;
          f3 = fu;
          fu = f.apply(u);
        }
      } else if ((u - uLim) * (uLim - x3) >= 0) {
        u = uLim;
        fu = f.apply(u);
      } else {
        u = x3 + MAGNIFICATION * (x3 - x2);
        fu = f.apply(u);
      }
      x1 = x2;
      x2 = x3;
      x3 = u;
      f1 = f2;
      f2 = f3;
      f3 = fu;
    }
    throw new MathException("Could not bracket a minimum in " + MAX_ITER + " attempts");
  }

}
