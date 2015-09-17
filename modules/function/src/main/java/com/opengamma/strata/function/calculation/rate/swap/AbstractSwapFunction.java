/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static com.opengamma.strata.engine.calculation.function.FunctionUtils.toScenarioResult;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.engine.calculation.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculation.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;

/**
 * Perform calculations on a single {@code SwapTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined from the first swap leg.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractSwapFunction<T>
    extends AbstractCalculationFunction<SwapTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractSwapFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractSwapFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingSwapProductPricer pricer() {
    return DiscountingSwapProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(SwapTrade trade) {
    Swap swap = trade.getProduct();
    Set<Index> indices = swap.allIndices();

    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());

    Set<MarketDataKey<?>> indexCurveKeys =
        indices.stream()
            .map(MarketDataKeys::indexCurve)
            .collect(toImmutableSet());

    Set<DiscountFactorsKey> discountCurveKeys =
        swap.getLegs().stream()
            .map(SwapLeg::getCurrency)
            .map(DiscountFactorsKey::of)
            .collect(toImmutableSet());

    return FunctionRequirements.builder()
        .singleValueRequirements(Sets.union(indexCurveKeys, discountCurveKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(swap.getLegs().stream().map(SwapLeg::getCurrency).collect(toImmutableSet()))
        .build();
  }

  @Override
  public ScenarioResult<T> execute(SwapTrade trade, CalculationMarketData marketData) {
    ExpandedSwap product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(SwapTrade target) {
    return Optional.of(target.getProduct().getLegs().get(0).getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedSwap product, RatesProvider provider);

}
