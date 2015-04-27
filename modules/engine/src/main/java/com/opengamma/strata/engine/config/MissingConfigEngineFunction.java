/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

/**
 * Function used when there is no function registered that can calculate a requested value.
 */
public class MissingConfigEngineFunction implements EngineSingleFunction<CalculationTarget, Object> {

  @Override
  public CalculationRequirements requirements(CalculationTarget target) {
    return CalculationRequirements.EMPTY;
  }

  @Override
  public Result<Object> execute(CalculationTarget target, CalculationMarketData marketData) {
    // TODO Pass in the measure and include it in the error message
    return Result.failure(FailureReason.INVALID_INPUT, "No configuration found to calculate a value for {}", target);
  }
}
