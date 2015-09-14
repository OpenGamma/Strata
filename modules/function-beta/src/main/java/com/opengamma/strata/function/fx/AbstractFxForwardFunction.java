/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import static com.opengamma.strata.engine.calculation.function.FunctionUtils.toScenarioResult;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.engine.calculation.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculation.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculation.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
import com.opengamma.strata.finance.fx.ExpandedFx;
import com.opengamma.strata.finance.fx.Fx;
import com.opengamma.strata.finance.fx.FxTrade;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.fx.DiscountingFxProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates a result for an {@code FxTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxForwardFunction<T>
    implements CalculationSingleFunction<FxTrade, ScenarioResult<T>> {

  /**
   * If this is true the value returned by the {@code execute} method will support automatic currency
   * conversion if the underlying results support it.
   */
  private final boolean convertCurrencies;

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractFxForwardFunction() {
    this(true);
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractFxForwardFunction(boolean convertCurrencies) {
    this.convertCurrencies = convertCurrencies;
  }

  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxProductPricer pricer() {
    return DiscountingFxProductPricer.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(FxTrade trade) {
    Fx fx = trade.getProduct();
    Currency baseCurrency = fx.getBaseCurrencyAmount().getCurrency();
    Currency counterCurrency = fx.getCounterCurrencyAmount().getCurrency();

    Set<DiscountFactorsKey> discountCurveKeys =
        ImmutableSet.of(DiscountFactorsKey.of(baseCurrency), DiscountFactorsKey.of(counterCurrency));

    return FunctionRequirements.builder()
        .singleValueRequirements(discountCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(baseCurrency, counterCurrency)
        .build();
  }

  @Override
  public ScenarioResult<T> execute(FxTrade trade, CalculationMarketData marketData) {
    ExpandedFx product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(convertCurrencies));
  }

  /**
   * Returns the base currency of the market convention pair of the trade currencies.
   *
   * @param target  the target of the calculation
   * @return the base currency of the market convention pair of the trade currencies
   */
  @Override
  public Optional<Currency> defaultReportingCurrency(FxTrade target) {
    Currency base = target.getProduct().getBaseCurrencyAmount().getCurrency();
    Currency counter = target.getProduct().getCounterCurrencyAmount().getCurrency();
    CurrencyPair marketConventionPair = CurrencyPair.of(base, counter).toConventional();
    return Optional.of(marketConventionPair.getBase());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFx product, RatesProvider provider);

}
