/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.collect.ArgChecker;

/**
 * If a model parameter $x$ is constrained to be either above or below some
 * level $a$ (i.e. $x > a$ or $x < a$), the function to transform it to an
 * unconstrained variable $y$ is given by
 * $$
 * \begin{align*}
 * y = 
 * \begin{cases}
 * \ln(e^{x-a} - 1)\quad & x > a\\
 * a - \ln(e^{a-x} - 1)\quad & x < a
 * \end{cases}
 * \end{align*}
 * $$
 * with inverse transform
 * $$
 * \begin{align*}
 * x = 
 * \begin{cases}
 * a + \ln(e^y + 1)\quad & x > a\\
 * a - \ln(e^y + 1)\quad & x < a
 * \end{cases}
 * \end{align*}
 * $$
 * For large $y > 50$, this becomes
 * $$
 * \begin{align*}
 * y = 
 * \begin{cases}
 * x - a\quad & x > a\\
 * a - x\quad & x < a
 * \end{cases}
 * \end{align*}
 * $$
 * with inverse transform
 * $$
 * \begin{align*}
 * x = 
 * \begin{cases}
 * a + y\quad & x > a\\
 * a - y\quad & x < a
 * \end{cases}
 * \end{align*}
 * $$
 * so any value of $y$ will give a value of $x$.
 */
public class SingleRangeLimitTransform implements ParameterLimitsTransform {

  private static final double EXP_MAX = 50.;
  private final double _limit;
  private final int _sign;

  /**
   * @param a The limit level 
   * @param limitType Type of the limit for the parameter
   */
  public SingleRangeLimitTransform(double a, LimitType limitType) {
    _limit = a;
    _sign = limitType == LimitType.GREATER_THAN ? 1 : -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double inverseTransform(double y) {
    if (y > EXP_MAX) {
      return _limit + _sign * y;
    } else if (y < -EXP_MAX) {
      return _limit;
    }
    return _limit + _sign * Math.log(Math.exp(y) + 1);
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If the value of $x$ is not consistent with the limit
   *  (e.g. the limit is $x > a$ and $x$ is less than $a$
   */
  @Override
  public double transform(double x) {
    ArgChecker.isTrue(_sign * x >= _sign * _limit, "x not in limit");
    if (x == _limit) {
      return -EXP_MAX;
    }
    double r = _sign * (x - _limit);
    if (r > EXP_MAX) {
      return r;
    }
    return Math.log(Math.exp(r) - 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double inverseTransformGradient(double y) {
    if (y > EXP_MAX) {
      return _sign;
    }
    double temp = Math.exp(y);
    return _sign * temp / (temp + 1);
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If the value of $x$ is not consistent with the limit
   *  (e.g. the limit is $x > a$ and $x$ is less than $a$
   */
  @Override
  public double transformGradient(double x) {
    ArgChecker.isTrue(_sign * x >= _sign * _limit, "x not in limit");
    double r = _sign * (x - _limit);
    if (r > EXP_MAX) {
      return 1.0;
    }
    double temp = Math.exp(r);
    return _sign * temp / (temp - 1);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_limit);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + _sign;
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
    SingleRangeLimitTransform other = (SingleRangeLimitTransform) obj;
    if (Double.doubleToLongBits(_limit) != Double.doubleToLongBits(other._limit)) {
      return false;
    }
    return _sign == other._sign;
  }

}
