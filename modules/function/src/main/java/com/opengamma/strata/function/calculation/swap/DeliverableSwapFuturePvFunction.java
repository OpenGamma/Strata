/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;

/**
 * Calculates the present value of a {@code DeliverableSwapFutureTrade} for each of a set of scenarios.
 */
public class DeliverableSwapFuturePvFunction
    extends AbstractDeliverableSwapFutureFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(DeliverableSwapFutureTrade trade, SingleCalculationMarketData marketData) {
    QuoteKey key = QuoteKey.of(trade.getSecurity().getStandardId());
    double price = marketData.getValue(key) / 100;  // convert market quote to value needed
    return pricer().presentValue(trade, new MarketDataRatesProvider(marketData), price);
  }

}
