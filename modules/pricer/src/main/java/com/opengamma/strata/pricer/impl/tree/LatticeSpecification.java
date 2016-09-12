/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Lattice specification interface.
 * <p>
 * An implementation of the lattice specification defines construction of binomial and trinomial trees, and computes
 * transition probabilities and state steps.
 * <p>
 * Reference: Y. Iwashita, "Tree Option Pricing Models" OpenGamma Quantitative Research 23.
 */
public interface LatticeSpecification {

  /**
   * Computes parameters for uniform trinomial tree.
   * <p>
   * The interest rate must be zero-coupon continuously compounded rate.
   * <p>
   * The trinomial tree parameters are represented as {@code DoubleArray} containing [0] up factor, [1] middle factor, 
   * [2] down factor, [3] up probability, [4] middle probability, [5] down probability.
   * 
   * @param volatility  the volatility 
   * @param interestRate  the interest rate
   * @param dt  the time step
   * @return the trinomial tree parameters
   */
  public abstract DoubleArray getParametersTrinomial(double volatility, double interestRate, double dt);

}
