/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

/**
 * Interface for probability distributions.
 * @param <T> Type of the parameters of the distribution
 */
// CSOFF: ALL
public interface ProbabilityDistribution<T> {

  /**
   * @return The next random number from this distribution
   */
  public abstract double nextRandom();

  /**
   * Return the probability density function for a value 
   * @param x The value, not null 
   * @return The pdf
   */
  public abstract double getPDF(T x);

  /**
   * Returns the cumulative distribution function for a value
   * @param x The value, not null
   * @return The cdf
   */
  public abstract double getCDF(T x);

  /**
   * Given a probability, return the value that returns this cdf
   * @param p The probability, not null. $0 \geq p \geq 1$
   * @return The inverse cdf
   */
  public abstract double getInverseCDF(T p);

}
