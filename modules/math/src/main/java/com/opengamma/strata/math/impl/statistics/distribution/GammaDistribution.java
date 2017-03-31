/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import java.util.Date;

import com.opengamma.strata.collect.ArgChecker;

import cern.jet.random.Gamma;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

/**
 * The Gamma distribution is a continuous probability distribution with cdf
 * $$
 * \begin{align*}
 * F(x)=\frac{\gamma\left(k, \frac{x}{\theta}\right)}{\Gamma(k)}
 * \end{align*}
 * $$
 * and pdf
 * $$
 * \begin{align*}
 * f(x)=\frac{x^{k-1}e^{-\frac{x}{\theta}}}{\Gamma{k}\theta^k}
 * \end{align*}
 * $$
 * where $k$ is the shape parameter and $\theta$ is the scale parameter.
 * <p>
 */
public class GammaDistribution implements ProbabilityDistribution<Double> {

  private final Gamma _gamma;
  private final double _k;
  private final double _theta;

  /**
   * @param k The shape parameter of the distribution, not negative or zero
   * @param theta The scale parameter of the distribution, not negative or zero
   */
  public GammaDistribution(double k, double theta) {
    this(k, theta, new MersenneTwister(new Date()));
  }

  /**
   * @param k The shape parameter of the distribution, not negative or zero
   * @param theta The scale parameter of the distribution, not negative or zero
   * @param engine A uniform random number generator, not null
   */
  public GammaDistribution(double k, double theta, RandomEngine engine) {
    ArgChecker.isTrue(k > 0, "k must be > 0");
    ArgChecker.isTrue(theta > 0, "theta must be > 0");
    ArgChecker.notNull(engine, "engine");
    _gamma = new Gamma(k, 1. / theta, engine);
    _k = k;
    _theta = theta;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCDF(Double x) {
    ArgChecker.notNull(x, "x");
    return _gamma.cdf(x);
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
   */
  @Override
  public double getPDF(Double x) {
    ArgChecker.notNull(x, "x");
    return _gamma.pdf(x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double nextRandom() {
    return _gamma.nextDouble();
  }

  /**
   * @return The shape parameter
   */
  public double getK() {
    return _k;
  }

  /**
   * @return The location parameter
   */
  public double getTheta() {
    return _theta;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_k);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_theta);
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
    GammaDistribution other = (GammaDistribution) obj;
    if (Double.doubleToLongBits(_k) != Double.doubleToLongBits(other._k)) {
      return false;
    }
    return Double.doubleToLongBits(_theta) == Double.doubleToLongBits(other._theta);
  }

}
