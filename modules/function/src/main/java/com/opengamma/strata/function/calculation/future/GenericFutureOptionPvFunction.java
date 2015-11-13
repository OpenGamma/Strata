/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.future.GenericFutureOption;
import com.opengamma.strata.product.future.GenericFutureOptionTrade;

/**
 * Calculates the present value of a {@code GenericFutureOptionTrade} for each of a set of scenarios.
 */
public class GenericFutureOptionPvFunction
    extends AbstractGenericFutureOptionFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(GenericFutureOptionTrade trade, SingleCalculationMarketData marketData) {
    QuoteKey key = QuoteKey.of(trade.getSecurity().getStandardId());
    GenericFutureOption product = trade.getProduct();
    double price = marketData.getValue(key);
    double tickSize = product.getTickSize();
    double tickValue = product.getTickValue().getAmount();
    double unitPv = (price / tickSize) * tickValue;
    double pv = unitPv * trade.getQuantity();
    return CurrencyAmount.of(product.getCurrency(), pv);
  }

}
