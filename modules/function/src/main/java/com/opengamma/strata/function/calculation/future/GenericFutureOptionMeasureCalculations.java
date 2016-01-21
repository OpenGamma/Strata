/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.future;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toCurrencyValuesArray;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.future.GenericFutureOption;
import com.opengamma.strata.product.future.GenericFutureOptionTrade;

/**
 * Multi-scenario measure calculations for Future Option trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
class GenericFutureOptionMeasureCalculations {

  // restricted constructor
  private GenericFutureOptionMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      GenericFutureOptionTrade trade,
      CalculationMarketData marketData) {

    return marketDataStream(marketData)
        .map(market -> calculatePresentValue(trade, market))
        .collect(toCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      GenericFutureOptionTrade trade,
      MarketData marketData) {

    QuoteKey key = QuoteKey.of(trade.getSecurity().getStandardId());
    GenericFutureOption product = trade.getProduct();
    double price = marketData.getValue(key);
    double tickSize = product.getTickSize();
    double tickValue = product.getTickValue().getAmount();
    double unitPv = (price / tickSize) * tickValue;
    double pv = unitPv * trade.getQuantity();
    return CurrencyAmount.of(product.getCurrency(), pv);
  }

  //-------------------------------------------------------------------------
  // common code, creating a stream of MarketData from CalculationMarketData
  private static Stream<MarketData> marketDataStream(CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new SingleCalculationMarketData(marketData, index));
  }

}
