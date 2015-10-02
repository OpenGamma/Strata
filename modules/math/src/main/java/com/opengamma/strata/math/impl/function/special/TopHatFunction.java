/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;

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
public class TopHatFunction extends Function1D<Double, Double> {
  private final double _x1;
  private final double _x2;
  private final double _y;

  /**
   * @param x1 The lower edge 
   * @param x2 The upper edge, must be greater than x1
   * @param y The height 
   */
  public TopHatFunction(final double x1, final double x2, final double y) {
    ArgChecker.isTrue(x1 < x2, "x1 must be less than x2");
    _x1 = x1;
    _x2 = x2;
    _y = y;
  }

  /**
   * @param x The argument of the function, not null. Must have $x_1 < x < x_2$
   * @return The value of the function
   */
  @Override
  public Double evaluate(final Double x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x != _x1, "Function is undefined for x = x1");
    ArgChecker.isTrue(x != _x2, "Function is undefined for x = x2");
    if (x > _x1 && x < _x2) {
      return _y;
    }
    return 0.;
  }

}
