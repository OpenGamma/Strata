/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import static com.opengamma.strata.math.MathUtils.pow2;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Cox-Ross-Rubinstein lattice specification.
 */
public final class CoxRossRubinsteinLatticeSpecification implements LatticeSpecification {

  @Override
  public DoubleArray getParametersTrinomial(double volatility, double interestRate, double dt) {
    double dx = volatility * Math.sqrt(2d * dt);
    double upFactor = Math.exp(dx);
    double downFactor = Math.exp(-dx);
    double factor1 = Math.exp(0.5 * interestRate * dt);
    double factor2 = Math.exp(0.5 * dx);
    double factor3 = Math.exp(-0.5 * dx);
    double upProbability = pow2((factor1 - factor3) / (factor3 - factor2));
    double downProbability = pow2((factor2 - factor1) / (factor3 - factor2));
    double middleProbability = 1d - upProbability - downProbability;
    return DoubleArray.of(upFactor, 1d, downFactor, upProbability, middleProbability, downProbability);
  }

}
