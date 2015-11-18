/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;

import java.util.Optional;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IborIndexRatesKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Perform calculations on a single {@code IborFutureTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractIborFutureFunction<T>
    extends AbstractCalculationFunction<IborFutureTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractIborFutureFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractIborFutureFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingIborFutureTradePricer pricer() {
    return DiscountingIborFutureTradePricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(IborFutureTrade trade) {
    IborFuture product = trade.getProduct();

    // the market data that is needed
    QuoteKey quoteKey = QuoteKey.of(trade.getSecurity().getStandardId());
    IborIndexRatesKey indexForwardCurveKey = IborIndexRatesKey.of(product.getIndex());
    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(product.getCurrency());
    IndexRateKey indexTimeSeriesKey = IndexRateKey.of(product.getIndex());
    return FunctionRequirements.builder()
        .singleValueRequirements(quoteKey, indexForwardCurveKey, discountFactorsKey)
        .timeSeriesRequirements(indexTimeSeriesKey)
        .outputCurrencies(product.getCurrency())
        .build();
  }

  @Override
  public ScenarioResult<T> execute(IborFutureTrade trade, CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(md -> execute(trade, md))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(IborFutureTrade target) {
    return Optional.of(target.getProduct().getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(IborFutureTrade trade, SingleCalculationMarketData marketData);

}
