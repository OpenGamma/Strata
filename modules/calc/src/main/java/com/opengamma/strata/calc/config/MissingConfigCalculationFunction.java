/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;

/**
 * Function used when there is no function registered that can calculate a requested value.
 */
public class MissingConfigCalculationFunction implements CalculationSingleFunction<CalculationTarget, Void> {

  @Override
  public FunctionRequirements requirements(CalculationTarget target) {
    return FunctionRequirements.empty();
  }

  @Override
  public Void execute(CalculationTarget target, CalculationMarketData marketData) {
    // TODO Pass in the measure and include it in the error message
    throw new IllegalStateException("No pricing rule configured");
  }
}
