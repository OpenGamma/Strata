/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * Returns the notional amount of a {@code SwapTrade}.
 */
public class SwapTradeNotionalFunction implements EngineSingleFunction<SwapTrade, CurrencyAmount> {

  @Override
  public CalculationRequirements requirements(SwapTrade target) {
    return CalculationRequirements.EMPTY;
  }

  @Override
  public CurrencyAmount execute(SwapTrade input, CalculationMarketData marketData) {
    NotionalSchedule notionalSchedule = getNotionalSchedule(input);
    return CurrencyAmount.of(notionalSchedule.getCurrency(), notionalSchedule.getAmount().getInitialValue());
  }
  
  private NotionalSchedule getNotionalSchedule(SwapTrade input) {
    SwapLeg leg = input.getProduct().getLegs().stream().findFirst().get();
    if (leg instanceof RateCalculationSwapLeg) {
      RateCalculationSwapLeg rateCalculationSwapLeg = (RateCalculationSwapLeg) leg;
      return rateCalculationSwapLeg.getNotionalSchedule();
    }
    throw new IllegalArgumentException("Unsupported swap");
  }

}
