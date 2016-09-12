/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Class representing the top-hat function, defined as:
 * $$
 * \begin{align*}
 * T(x)=
 * \begin{cases}
 * 0 & x < x_1\\
 * y & x_1 < x < x_2\\
 * 0 & x > x_2
 * \end{cases}
 * \end{align*}
 * $$
 * where $x_1$ is the lower edge of the "hat", $x_2$ is the upper edge and $y$
 * is the height of the function.
 * 
 * This function is discontinuous at $x_1$ and $x_2$.
 */
public class TopHatFunction implements Function<Double, Double> {

  private final double _x1;
  private final double _x2;
  private final double _y;

  /**
   * Creates an instance.
   * 
   * @param x1  the lower edge 
   * @param x2  the upper edge, must be greater than x1
   * @param y  the height 
   */
  public TopHatFunction(double x1, double x2, double y) {
    ArgChecker.isTrue(x1 < x2, "x1 must be less than x2");
    _x1 = x1;
    _x2 = x2;
    _y = y;
  }

  //-------------------------------------------------------------------------
  /**
   * Evaluates the function.
   * 
   * @param x The argument of the function, not null. Must have $x_1 < x < x_2$
   * @return The value of the function
   */
  @Override
  public Double apply(Double x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x != _x1, "Function is undefined for x = x1");
    ArgChecker.isTrue(x != _x2, "Function is undefined for x = x2");
    if (x > _x1 && x < _x2) {
      return _y;
    }
    return 0.;
  }

}
