/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import java.time.LocalDate;

import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.Trade;

/**
 * Returns the trade date of a trade.
 */
public class TradeDateFunction
    implements EngineSingleFunction<Trade, LocalDate> {

  @Override
  public CalculationRequirements requirements(Trade target) {
    return CalculationRequirements.EMPTY;
  }

  @Override
  public LocalDate execute(Trade input, CalculationMarketData marketData) {
    return input.getTradeInfo().getTradeDate().orElse(null);
  }

}
