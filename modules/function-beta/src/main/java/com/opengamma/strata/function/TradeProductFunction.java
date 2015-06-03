/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.Product;
import com.opengamma.strata.finance.ProductTrade;
import com.opengamma.strata.finance.Trade;

/**
 * Returns the product from a trade.
 */
public class TradeProductFunction
    implements CalculationSingleFunction<Trade, Product> {

  @Override
  public CalculationRequirements requirements(Trade target) {
    return CalculationRequirements.empty();
  }

  @Override
  public Product execute(Trade target, CalculationMarketData marketData) {
    if (target instanceof ProductTrade) {
      return ((ProductTrade<?>) target).getProduct();
    }
    throw new UnsupportedOperationException(
        Messages.format("Unable to retrieve product from trade of type {}", target.getClass()));
  }

}
