/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 * The generalized extreme value distribution is a family of continuous probability distributions that combines the Gumbel (type I),
 * Fr&eacute;chet (type II) and Weibull (type III) families of distributions.
 * <p>
 * This distribution has location parameter $\mu$, shape parameter $\xi$
 * and scale parameter $\sigma$, with
 * $$
 * \begin{align*}
 * \mu&\in\Re,\\
 * \xi&\in\Re,\\
 * \sigma&>0
 * \end{align*}
 * $$
 * and support
 * $$
 * \begin{align*}
 * x\in
 * \begin{cases}
 * \left[\mu - \frac{\sigma}{\xi}, +\infty\right) & \text{when } \xi > 0\\
 * (-\infty,+\infty) & \text{when } \xi = 0\\\\
 * \left(-\infty, \mu - \frac{\sigma}{\xi}\right] & \text{when } \xi < 0
 * \end{cases}
 * \end{align*}
 * $$
 * The cdf is given by:
 * $$
 * \begin{align*}
 * F(x) &=e^{-t(x)}\\
 * t(x)&=
 * \begin{cases}
 * \left(1 + \xi\frac{x-\mu}{\sigma}\right)^{-\frac{1}{\xi}} & \text{if } \xi \neq 0,\\
 * e^{-\frac{x-\mu}{\sigma}} & \text{if } \xi = 0.
 * \end{cases}
 * \end{align*}
 * $$
 * and the pdf by:
 * $$
 * \begin{align*}
 * f(x)&=\frac{t(x)^{\xi + 1}e^{-t(x)}}{\sigma}\quad\\
 * t(x)&=
 * \begin{cases}
 * \left(1 + \xi\frac{x-\mu}{\sigma}\right)^{-\frac{1}{\xi}} & \text{if } \xi \neq 0,\\
 * e^{-\frac{x-\mu}{\sigma}} & \text{if } \xi = 0.
 * \end{cases}
 * \end{align*}
 * $$
 * 
 */
public class GeneralizedExtremeValueDistribution implements ProbabilityDistribution<Double> {

  private final double _mu;
  private final double _sigma;
  private final double _ksi;
  private final boolean _ksiIsZero;

  /**
   * Creates an instance.
   * 
   * @param mu The location parameter
   * @param sigma The scale parameter, not negative or zero
   * @param ksi The shape parameter
   */
  public GeneralizedExtremeValueDistribution(double mu, double sigma, double ksi) {
    ArgChecker.isTrue(sigma >= 0, "sigma must be >= 0");
    _mu = mu;
    _sigma = sigma;
    _ksi = ksi;
    _ksiIsZero = DoubleMath.fuzzyEquals(ksi, 0d, 1e-13);
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If $x \not\in$ support
   */
  @Override
  public double getCDF(Double x) {
    ArgChecker.notNull(x, "x");
    return Math.exp(-getT(x));
  }

  /**
   * {@inheritDoc}
   * @return Not supported
   * @throws UnsupportedOperationException always
   */
  @Override
  public double getInverseCDF(Double p) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If $x \not\in$ support
   */
  @Override
  public double getPDF(Double x) {
    ArgChecker.notNull(x, "x");
    double t = getT(x);
    return Math.pow(t, _ksi + 1) * Math.exp(-t) / _sigma;
  }

  /**
   * {@inheritDoc}
   * @return Not supported
   * @throws UnsupportedOperationException always
   */
  @Override
  public double nextRandom() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the location parameter.
   * 
   * @return The location parameter
   */
  public double getMu() {
    return _mu;
  }

  /**
   * Gets the scale parameter.
   * 
   * @return The scale parameter
   */
  public double getSigma() {
    return _sigma;
  }

  /**
   * Gets the shape parameter.
   * 
   * @return The shape parameter
   */
  public double getKsi() {
    return _ksi;
  }

  private double getT(double x) {
    if (_ksiIsZero) {
      return Math.exp(-(x - _mu) / _sigma);
    }
    if (_ksi < 0 && x > _mu - _sigma / _ksi) {
      throw new IllegalArgumentException("Support for GEV is in the range -infinity -> mu - sigma / ksi when ksi < 0");
    }
    if (_ksi > 0 && x < _mu - _sigma / _ksi) {
      throw new IllegalArgumentException("Support for GEV is in the range mu - sigma / ksi -> +infinity when ksi > 0");
    }
    return Math.pow(1 + _ksi * (x - _mu) / _sigma, -1. / _ksi);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_ksi);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_mu);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_sigma);
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
    GeneralizedExtremeValueDistribution other = (GeneralizedExtremeValueDistribution) obj;
    if (Double.doubleToLongBits(_ksi) != Double.doubleToLongBits(other._ksi)) {
      return false;
    }
    if (Double.doubleToLongBits(_mu) != Double.doubleToLongBits(other._mu)) {
      return false;
    }
    return Double.doubleToLongBits(_sigma) == Double.doubleToLongBits(other._sigma);
  }

}
