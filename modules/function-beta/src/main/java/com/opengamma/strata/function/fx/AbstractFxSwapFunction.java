/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toScenarioResult;

import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.fx.ExpandedFxSwap;
import com.opengamma.strata.finance.fx.FxSwap;
import com.opengamma.strata.finance.fx.FxSwapTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricerBeta;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates a result for an {@code FxSwapTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxSwapFunction<T>
    implements CalculationSingleFunction<FxSwapTrade, ScenarioResult<T>> {

  /**
   * If this is true the value returned by the {@code execute} method will support automatic currency
   * conversion if the underlying results support it.
   */
  private final boolean convertCurrencies;

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractFxSwapFunction() {
    this(true);
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractFxSwapFunction(boolean convertCurrencies) {
    this.convertCurrencies = convertCurrencies;
  }

  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxSwapProductPricerBeta pricer() {
    return DiscountingFxSwapProductPricerBeta.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRequirements requirements(FxSwapTrade trade) {
    FxSwap fx = trade.getProduct();
    Currency baseCurrency = fx.getNearLeg().getBaseCurrencyAmount().getCurrency();
    Currency counterCurrency = fx.getNearLeg().getCounterCurrencyAmount().getCurrency();

    Set<DiscountFactorsKey> discountCurveKeys =
        ImmutableSet.of(DiscountFactorsKey.of(baseCurrency), DiscountFactorsKey.of(counterCurrency));

    return CalculationRequirements.builder()
        .singleValueRequirements(discountCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(baseCurrency, counterCurrency)
        .build();
  }

  @Override
  public ScenarioResult<T> execute(FxSwapTrade trade, CalculationMarketData marketData) {
    ExpandedFxSwap product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(convertCurrencies));
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFxSwap product, RatesProvider provider);

}
