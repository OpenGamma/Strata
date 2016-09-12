/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.TrigonometricFunctionUtils;

/**
 * If a model parameter $x$ is constrained to be between two values $a \geq x
 * \geq b$, the function to transform it to an unconstrained variable is $y$ is
 * given by
 * $$
 * \begin{align*}
 * y &= \tanh^{-1}\left(\frac{x - m}{s}\right)\\
 * m &= \frac{a + b}{2}\\
 * s &= \frac{b - a}{2}
 * \end{align*}
 * $$
 * with the inverse transform
 * $$
 * \begin{align*}
 * x &= s\tanh(y) + m\\
 * \end{align*}
 * $$
 */
public class DoubleRangeLimitTransform implements ParameterLimitsTransform {

  private static final double TANH_MAX = 25.0;
  private final double _lower;
  private final double _upper;
  private final double _scale;
  private final double _mid;

  /**
   * @param lower Lower limit
   * @param upper Upper limit
   * @throws IllegalArgumentException If the upper limit is not greater than the lower limit
   */
  public DoubleRangeLimitTransform(double lower, double upper) {
    ArgChecker.isTrue(upper > lower, "upper limit must be greater than lower");
    _lower = lower;
    _upper = upper;
    _mid = (lower + upper) / 2;
    _scale = (upper - lower) / 2;
  }

  /**
   * If $y > 25$, this returns $b$. If $y < -25$ returns $a$.
   * {@inheritDoc}
   */
  @Override
  public double inverseTransform(double y) {
    if (y > TANH_MAX) {
      return _upper;
    } else if (y < -TANH_MAX) {
      return _lower;
    }
    return _mid + _scale * TrigonometricFunctionUtils.tanh(y);
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If $x > b$ or $x < a$
   */
  @Override
  public double transform(double x) {
    ArgChecker.isTrue(x <= _upper && x >= _lower, "parameter out of range");
    if (x == _upper) {
      return TANH_MAX;
    } else if (x == _lower) {
      return -TANH_MAX;
    }
    return TrigonometricFunctionUtils.atanh((x - _mid) / _scale);
  }

  /**
   * If $|y| > 25$, this returns 0.
   * {@inheritDoc}
   */
  @Override
  public double inverseTransformGradient(double y) {
    if (y > TANH_MAX || y < -TANH_MAX) {
      return 0.0;
    }
    double p = 2 * y;
    double ep = Math.exp(p);
    double epp1 = ep + 1;
    return _scale * 4 * ep / (epp1 * epp1);
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If $x > b$ or $x < a$
   */
  @Override
  public double transformGradient(double x) {
    ArgChecker.isTrue(x <= _upper && x >= _lower, "parameter out of range");
    double t = (x - _mid) / _scale;
    return 1 / (_scale * (1 - t * t));
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_lower);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_upper);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DoubleRangeLimitTransform other = (DoubleRangeLimitTransform) obj;
    if (Double.doubleToLongBits(_lower) != Double.doubleToLongBits(other._lower)) {
      return false;
    }
    return Double.doubleToLongBits(_upper) == Double.doubleToLongBits(other._upper);
  }

}
