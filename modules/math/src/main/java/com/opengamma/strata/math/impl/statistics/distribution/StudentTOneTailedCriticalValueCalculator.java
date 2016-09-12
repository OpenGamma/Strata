/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import java.util.function.Function;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

import cern.jet.random.engine.RandomEngine;

/**
 *
 */
public class StudentTOneTailedCriticalValueCalculator implements Function<Double, Double> {

  private final ProbabilityDistribution<Double> _dist;

  public StudentTOneTailedCriticalValueCalculator(double nu) {
    ArgChecker.notNegative(nu, "nu");
    _dist = new StudentTDistribution(nu);
  }

  public StudentTOneTailedCriticalValueCalculator(double nu, RandomEngine engine) {
    ArgChecker.notNegative(nu, "nu");
    ArgChecker.notNull(engine, "engine");
    _dist = new StudentTDistribution(nu, engine);
  }

  @Override
  public Double apply(Double x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.notNegative(x, "x");
    if (DoubleMath.fuzzyEquals(x, 0.5, 1e-14)) {
      return 0.5;
    }
    return _dist.getInverseCDF(x);
  }

}
