/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import java.time.LocalDate;

import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.Trade;

/**
 * Returns the settlement date of a trade.
 */
public class TradeSettlementDateFunction
    implements CalculationSingleFunction<Trade, LocalDate> {

  @Override
  public CalculationRequirements requirements(Trade target) {
    return CalculationRequirements.empty();
  }

  @Override
  public LocalDate execute(Trade input, CalculationMarketData marketData) {
    return input.getTradeInfo().getSettlementDate().orElse(null);
  }

}
