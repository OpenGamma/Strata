/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
/*
 * This code is copied from the original library from the `cern.jet.random` package.
 * Changes:
 * - package name
 * - missing Javadoc param tags
 * - reformat
 * - remove unused method
 */
/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
*/
package com.opengamma.strata.math.impl.cern;

//CSOFF: ALL
/**
Normal (aka Gaussian) distribution; See the <A HREF="http://www.cern.ch/RD11/rkb/AN16pp/node188.html#SECTION0001880000000000000000"> math definition</A>
and <A HREF="http://www.statsoft.com/textbook/glosn.html#Normal Distribution"> animated definition</A>.
<pre>                       
				   1                       2
	  pdf(x) = ---------    exp( - (x-mean) / 2v ) 
			   sqrt(2pi*v)

							x
							 -
				   1        | |                 2
	  cdf(x) = ---------    |    exp( - (t-mean) / 2v ) dt
			   sqrt(2pi*v)| |
						   -
						  -inf.
</pre>
where <tt>v = variance = standardDeviation^2</tt>.
<p>
Instance methods operate on a user supplied uniform random number generator; they are unsynchronized.
<dt>
Static methods operate on a default uniform random number generator; they are synchronized.
<p>
<b>Implementation:</b> Polar Box-Muller transformation. See 
G.E.P. Box, M.E. Muller (1958): A note on the generation of random normal deviates, Annals Math. Statist. 29, 610-611.
<p>
@author wolfgang.hoschek@cern.ch
@version 1.0, 09/24/99
*/
public class Normal extends AbstractContinousDistribution {

  private static final long serialVersionUID = 1L;

  protected double mean;
  protected double variance;
  protected double standardDeviation;

  protected double cache; // cache for Box-Mueller algorithm 
  protected boolean cacheFilled; // Box-Mueller

  protected double SQRT_INV; // performance cache

  // The uniform random number generated shared by all <b>static</b> methods.
  protected static Normal shared = new Normal(0.0, 1.0, makeDefaultGenerator());

  /**
   * Constructs a normal (gauss) distribution.
   * Example: mean=0.0, standardDeviation=1.0.
   * @param mean mean
   * @param standardDeviation standard deviation
   * @param randomGenerator generator
   */
  public Normal(double mean, double standardDeviation, RandomEngine randomGenerator) {
    setRandomGenerator(randomGenerator);
    setState(mean, standardDeviation);
  }

  /**
   * Returns the cumulative distribution function.
   * @param x x
   * @return result
   */
  public double cdf(double x) {
    return Probability.normal(mean, variance, x);
  }

  /**
   * Returns a random number from the distribution.
   */
  @Override
  public double nextDouble() {
    return nextDouble(this.mean, this.standardDeviation);
  }

  /**
   * Returns a random number from the distribution; bypasses the internal state.
   * @param mean mean
   * @param standardDeviation standard deviation
   * @return result
   */
  public double nextDouble(double mean, double standardDeviation) {
    // Uses polar Box-Muller transformation.
    if (cacheFilled && this.mean == mean && this.standardDeviation == standardDeviation) {
      cacheFilled = false;
      return cache;
    }
    ;

    double x, y, r, z;
    do {
      x = 2.0 * randomGenerator.raw() - 1.0;
      y = 2.0 * randomGenerator.raw() - 1.0;
      r = x * x + y * y;
    } while (r >= 1.0);

    z = Math.sqrt(-2.0 * Math.log(r) / r);
    cache = mean + standardDeviation * x * z;
    cacheFilled = true;
    return mean + standardDeviation * y * z;
  }

  /**
   * Returns the probability distribution function.
   * @param x x
   * @return result
   */
  public double pdf(double x) {
    double diff = x - mean;
    return SQRT_INV * Math.exp(-(diff * diff) / (2.0 * variance));
  }

  /**
   * Sets the uniform random generator internally used.
   */
  @Override
  protected void setRandomGenerator(RandomEngine randomGenerator) {
    super.setRandomGenerator(randomGenerator);
    this.cacheFilled = false;
  }

  /**
   * Sets the mean and variance.
   * @param mean mean
   * @param standardDeviation standard deviation
   */
  public void setState(double mean, double standardDeviation) {
    if (mean != this.mean || standardDeviation != this.standardDeviation) {
      this.mean = mean;
      this.standardDeviation = standardDeviation;
      this.variance = standardDeviation * standardDeviation;
      this.cacheFilled = false;

      this.SQRT_INV = 1.0 / Math.sqrt(2.0 * Math.PI * variance);
    }
  }

  /**
   * Returns a random number from the distribution with the given mean and standard deviation.
   * @param mean mean
   * @param standardDeviation standard deviation
   * @return result
   */
  public static double staticNextDouble(double mean, double standardDeviation) {
    synchronized (shared) {
      return shared.nextDouble(mean, standardDeviation);
    }
  }

  /**
   * Returns a String representation of the receiver.
   */
  @Override
  public String toString() {
    return this.getClass().getName() + "(" + mean + "," + standardDeviation + ")";
  }

}
