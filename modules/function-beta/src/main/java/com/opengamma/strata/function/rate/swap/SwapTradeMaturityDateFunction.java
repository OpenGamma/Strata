/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import java.time.LocalDate;
import java.util.Comparator;

import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * Returns the maturity date of a {@code SwapTrade}.
 */
public class SwapTradeMaturityDateFunction
    implements EngineSingleFunction<SwapTrade, LocalDate> {

  @Override
  public CalculationRequirements requirements(SwapTrade target) {
    return CalculationRequirements.EMPTY;
  }

  @Override
  public LocalDate execute(SwapTrade input, CalculationMarketData marketData) {
    return input.getProduct().getLegs().stream()
        .map(SwapLeg::getEndDate)
        .max(Comparator.naturalOrder())
        .get();  // there is at least one leg
  }

}
