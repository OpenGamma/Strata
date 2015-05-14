/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.Trade;

/**
 * Returns the identifer of a trade.
 */
public class TradeIdFunction
    implements CalculationSingleFunction<Trade, StandardId> {

  @Override
  public CalculationRequirements requirements(Trade target) {
    return CalculationRequirements.empty();
  }

  @Override
  public StandardId execute(Trade input, CalculationMarketData marketData) {
    return input.getStandardId();
  }

}
