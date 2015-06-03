/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;

/**
 * Returns the standard block of trade information.
 */
public class TradeInfoFunction
    implements CalculationSingleFunction<Trade, TradeInfo>  {

  @Override
  public CalculationRequirements requirements(Trade target) {
    return CalculationRequirements.empty();
  }

  @Override
  public TradeInfo execute(Trade target, CalculationMarketData marketData) {
    return target.getTradeInfo();
  }

}
