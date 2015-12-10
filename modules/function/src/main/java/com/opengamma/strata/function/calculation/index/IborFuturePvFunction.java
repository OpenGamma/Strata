/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Calculates the present value of a {@code IborFutureTrade} for each of a set of scenarios.
 */
public class IborFuturePvFunction
    extends AbstractIborFutureFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(IborFutureTrade trade, MarketData marketData) {
    QuoteKey key = QuoteKey.of(trade.getSecurity().getStandardId());
    double price = marketData.getValue(key) / 100;  // convert market quote to value needed
    return pricer().presentValue(trade, new MarketDataRatesProvider(marketData), price);
  }

}
