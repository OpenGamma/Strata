/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

import cern.jet.random.engine.RandomEngine;

/**
 * StudentT calculator.
 */
// CSOFF: JavadocMethod
public class StudentTTwoTailedCriticalValueCalculator implements Function<Double, Double> {

  private final Function<Double, Double> _calc;

  public StudentTTwoTailedCriticalValueCalculator(double nu) {
    ArgChecker.notNegative(nu, "nu");
    _calc = new StudentTOneTailedCriticalValueCalculator(nu);
  }

  public StudentTTwoTailedCriticalValueCalculator(double nu, RandomEngine engine) {
    ArgChecker.notNegative(nu, "nu");
    ArgChecker.notNull(engine, "engine");
    _calc = new StudentTOneTailedCriticalValueCalculator(nu, engine);
  }

  @Override
  public Double apply(Double x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.notNegative(x, "x");
    return _calc.apply(0.5 + 0.5 * x);
  }

}
