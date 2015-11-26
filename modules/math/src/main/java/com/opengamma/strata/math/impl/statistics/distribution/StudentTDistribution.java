/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import java.util.Date;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.special.GammaFunction;
import com.opengamma.strata.math.impl.function.special.InverseIncompleteBetaFunction;

import cern.jet.random.StudentT;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

/**
 * Student's T-distribution is a continuous probability distribution with probability density function
 * $$
 * \begin{align*}
 * f(x) = \frac{\Gamma\left(\frac{\nu + 1}{2}\right)}{\sqrt{\nu\pi}\Gamma(\left(\frac{\nu}{2}\right)}\left(1 + \frac{x^2}{\nu}\right)^{-\frac{1}{2}(\nu + 1)}
 * \end{align*}
 * $$
 * where $\nu$ is the number of degrees of freedom and $\Gamma$ is the Gamma function ({@link GammaFunction}).
 */
public class StudentTDistribution implements ProbabilityDistribution<Double> {
  // TODO need a better seed
  private final double _degFreedom;
  private final StudentT _dist;
  private final Function<Double, Double> _beta;

  /**
   * @param degFreedom The number of degrees of freedom, not negative or zero
   */
  public StudentTDistribution(double degFreedom) {
    this(degFreedom, new MersenneTwister64(new Date()));
  }

  /**
   * @param degFreedom The number of degrees of freedom, not negative or zero
   * @param engine A generator of uniform random numbers, not null
   */
  public StudentTDistribution(double degFreedom, RandomEngine engine) {
    ArgChecker.isTrue(degFreedom > 0, "degrees of freedom");
    ArgChecker.notNull(engine, "engine");
    _degFreedom = degFreedom;
    _dist = new StudentT(degFreedom, engine);
    _beta = new InverseIncompleteBetaFunction(degFreedom / 2., 0.5);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCDF(Double x) {
    ArgChecker.notNull(x, "x");
    return _dist.cdf(x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getPDF(Double x) {
    ArgChecker.notNull(x, "x");
    return _dist.pdf(x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double nextRandom() {
    return _dist.nextDouble();
  }

  /**
   * {@inheritDoc}
   * The inverse cdf is given by:
   * $$
   * \begin{align*}
   * F(P) &= \mathrm{sign}(p - \frac{1}{2})\sqrt{\frac{\nu}{x - 1}}\\
   * x &= B(2 \min(p, 1-p)) 
   * \end{align*}
   * $$
   * where $B$ is the inverse incomplete Beta function ({@link InverseIncompleteBetaFunction}).
   */
  @Override
  public double getInverseCDF(Double p) {
    ArgChecker.notNull(p, "p");
    ArgChecker.isTrue(p >= 0 && p <= 1, "Probability must be >= 0 and <= 1");
    double x = _beta.apply(2 * Math.min(p, 1 - p));
    return Math.signum(p - 0.5) * Math.sqrt(_degFreedom * (1. / x - 1));
  }

  /**
   * @return The number of degrees of freedom
   */
  public double getDegreesOfFreedom() {
    return _degFreedom;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_degFreedom);
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
    StudentTDistribution other = (StudentTDistribution) obj;
    return Double.doubleToLongBits(_degFreedom) == Double.doubleToLongBits(other._degFreedom);
  }

}
