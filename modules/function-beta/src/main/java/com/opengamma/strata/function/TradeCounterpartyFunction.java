/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.Trade;

/**
 * Returns the counterparty of a trade.
 */
public class TradeCounterpartyFunction
    implements EngineSingleFunction<Trade, StandardId> {

  @Override
  public CalculationRequirements requirements(Trade target) {
    return CalculationRequirements.EMPTY;
  }

  @Override
  public StandardId execute(Trade input, CalculationMarketData marketData) {
    return input.getTradeInfo().getCounterparty().orElse(null);
  }

}
