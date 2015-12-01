/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.pricer.swap.DiscountingDeliverableSwapFutureTradePricer;
import com.opengamma.strata.product.swap.DeliverableSwapFuture;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;

/**
 * Perform calculations on a single {@code DeliverableSwapFutureTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractDeliverableSwapFutureFunction<T>
    extends AbstractCalculationFunction<DeliverableSwapFutureTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractDeliverableSwapFutureFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractDeliverableSwapFutureFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingDeliverableSwapFutureTradePricer pricer() {
    return DiscountingDeliverableSwapFutureTradePricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(DeliverableSwapFutureTrade trade) {
    DeliverableSwapFuture product = trade.getProduct();

    // the market data that is needed
    QuoteKey quoteKey = QuoteKey.of(trade.getSecurity().getStandardId());
    Set<Index> indices = product.getUnderlyingSwap().allIndices();
    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());
    Set<MarketDataKey<?>> indexCurveKeys =
        indices.stream()
            .map(MarketDataKeys::indexCurve)
            .collect(toImmutableSet());
    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(product.getCurrency());
    Set<MarketDataKey<?>> reqs = ImmutableSet.<MarketDataKey<?>>builder()
        .add(quoteKey)
        .add(discountFactorsKey)
        .addAll(indexCurveKeys)
        .build();
    return FunctionRequirements.builder()
        .singleValueRequirements(reqs)
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(product.getCurrency())
        .build();
  }

  @Override
  public ScenarioResult<T> execute(DeliverableSwapFutureTrade trade, CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(md -> execute(trade, md))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(DeliverableSwapFutureTrade target) {
    return Optional.of(target.getProduct().getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(DeliverableSwapFutureTrade trade, SingleCalculationMarketData marketData);

}
