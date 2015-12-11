/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.future.GenericFuture;
import com.opengamma.strata.product.future.GenericFutureTrade;

/**
 * Calculates the present value of a {@code GenericFutureTrade} for each of a set of scenarios.
 */
public class GenericFuturePvFunction
    extends AbstractGenericFutureFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(GenericFutureTrade trade, MarketData marketData) {
    QuoteKey key = QuoteKey.of(trade.getSecurity().getStandardId());
    GenericFuture product = trade.getProduct();
    double price = marketData.getValue(key);
    double tickSize = product.getTickSize();
    double tickValue = product.getTickValue().getAmount();
    double unitPv = (price / tickSize) * tickValue;
    double pv = unitPv * trade.getQuantity();
    return CurrencyAmount.of(product.getCurrency(), pv);
  }

}
