/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import java.util.Date;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

/**
 * 
 * The generalized Pareto distribution is a family of power law probability
 * distributions with location parameter $\mu$, shape parameter $\xi$ and scale
 * parameter $\sigma$, where
 * $$
 * \begin{eqnarray*}
 * \mu&\in&\Re,\\
 * \xi&\in&\Re,\\
 * \sigma&>&0
 * \end{eqnarray*}
 * $$
 * and with support
 * $$
 * \begin{eqnarray*}
 * x\geq\mu\quad\quad\quad(\xi\geq 0)\\
 * \mu\leq x\leq\mu-\frac{\sigma}{\xi}\quad(\xi<0)
 * \end{eqnarray*}
 * $$
 * The cdf is given by:
 * $$
 * \begin{align*}
 * F(z)&=1-\left(1 + \xi z\right)^{-\frac{1}{\xi}}\\
 * z&=\frac{x-\mu}{\sigma}
 * \end{align*}
 * $$
 * and the pdf is given by:
 * $$
 * \begin{align*}
 * f(z)&=\frac{\left(1+\xi z\right)^{-\left(\frac{1}{\xi} + 1\right)}}{\sigma}\\
 * z&=\frac{x-\mu}{\sigma}
 * \end{align*}
 * $$
 * Given a uniform random number variable $U$ drawn from the interval $(0,1]$, a
 * Pareto-distributed random variable with parameters $\mu$, $\sigma$ and
 * $\xi$ is given by
 * $$
 * \begin{align*}
 * X=\mu + \frac{\sigma\left(U^{-\xi}-1\right)}{\xi}\sim GPD(\mu,\sigma,\xi)
 * \end{align*}
 * $$
 */
public class GeneralizedParetoDistribution implements ProbabilityDistribution<Double> {
  // TODO check cdf, pdf for support
  private final double _mu;
  private final double _sigma;
  private final double _ksi;
  // TODO better seed
  private final RandomEngine _engine;

  /**
   * 
   * @param mu The location parameter
   * @param sigma The scale parameter, not negative or zero
   * @param ksi The shape parameter, not zero
   */
  public GeneralizedParetoDistribution(double mu, double sigma, double ksi) {
    this(mu, sigma, ksi, new MersenneTwister64(new Date()));
  }

  /**
   * 
   * @param mu The location parameter
   * @param sigma The scale parameter
   * @param ksi The shape parameter
   * @param engine A uniform random number generator, not null
   */
  public GeneralizedParetoDistribution(double mu, double sigma, double ksi, RandomEngine engine) {
    ArgChecker.isTrue(sigma > 0, "sigma must be > 0");
    ArgChecker.isTrue(!DoubleMath.fuzzyEquals(ksi, 0d, 1e-15), "ksi cannot be zero");
    ArgChecker.notNull(engine, "engine");
    _mu = mu;
    _sigma = sigma;
    _ksi = ksi;
    _engine = engine;
  }

  /**
   * @return The location parameter
   */
  public double getMu() {
    return _mu;
  }

  /**
   * @return The scale parameter
   */
  public double getSigma() {
    return _sigma;
  }

  /**
   * @return The shape parameter
   */
  public double getKsi() {
    return _ksi;
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException If $x \not\in$ support
   */
  @Override
  public double getCDF(Double x) {
    ArgChecker.notNull(x, "x");
    return 1 - Math.pow(1 + _ksi * getZ(x), -1. / _ksi);
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
    return Math.pow(1 + _ksi * getZ(x), -(1. / _ksi + 1)) / _sigma;
  }

  /**
   * {@inheritDoc} 
   */
  @Override
  public double nextRandom() {
    return _mu + _sigma * (Math.pow(_engine.nextDouble(), -_ksi) - 1) / _ksi;
  }

  private double getZ(double x) {
    if (_ksi > 0 && x < _mu) {
      throw new IllegalArgumentException("Support for GPD is in the range x >= mu if ksi > 0");
    }
    if (_ksi < 0 && (x <= _mu || x >= _mu - _sigma / _ksi)) {
      throw new IllegalArgumentException("Support for GPD is in the range mu <= x <= mu - sigma / ksi if ksi < 0");
    }
    return (x - _mu) / _sigma;
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
    GeneralizedParetoDistribution other = (GeneralizedParetoDistribution) obj;
    if (Double.doubleToLongBits(_ksi) != Double.doubleToLongBits(other._ksi)) {
      return false;
    }
    if (Double.doubleToLongBits(_mu) != Double.doubleToLongBits(other._mu)) {
      return false;
    }
    return Double.doubleToLongBits(_sigma) == Double.doubleToLongBits(other._sigma);
  }

}
