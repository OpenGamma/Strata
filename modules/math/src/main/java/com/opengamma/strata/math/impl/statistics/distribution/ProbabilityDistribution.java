/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

/**
 * Interface for probability distributions.
 * @param <T> Type of the parameters of the distribution
 */
public interface ProbabilityDistribution<T> {

  /**
   * @return The next random number from this distribution
   */
  double nextRandom();

  /**
   * Return the probability density function for a value 
   * @param x The value, not null 
   * @return The pdf
   */
  double getPDF(T x);

  /**
   * Returns the cumulative distribution function for a value
   * @param x The value, not null
   * @return The cdf
   */
  double getCDF(T x);

  /**
   * Given a probability, return the value that returns this cdf
   * @param p The probability, not null. $0 \geq p \geq 1$
   * @return The inverse cdf
   */
  double getInverseCDF(T p);

}
