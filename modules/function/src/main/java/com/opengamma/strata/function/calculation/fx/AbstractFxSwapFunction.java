/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.engine.calculation.function.FunctionUtils.toScenarioResult;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.engine.calculation.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculation.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
import com.opengamma.strata.finance.fx.ExpandedFxSwap;
import com.opengamma.strata.finance.fx.FxSwap;
import com.opengamma.strata.finance.fx.FxSwapTrade;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Perform calculations on a single {@code FxSwapTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined to be the base currency of the market convention
 * pair of the near leg currencies.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxSwapFunction<T>
    extends AbstractCalculationFunction<FxSwapTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractFxSwapFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractFxSwapFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxSwapProductPricer pricer() {
    return DiscountingFxSwapProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(FxSwapTrade trade) {
    FxSwap fx = trade.getProduct();
    Currency baseCurrency = fx.getNearLeg().getBaseCurrencyAmount().getCurrency();
    Currency counterCurrency = fx.getNearLeg().getCounterCurrencyAmount().getCurrency();

    Set<DiscountFactorsKey> discountCurveKeys =
        ImmutableSet.of(DiscountFactorsKey.of(baseCurrency), DiscountFactorsKey.of(counterCurrency));

    return FunctionRequirements.builder()
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
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(FxSwapTrade target) {
    Currency base = target.getProduct().getNearLeg().getBaseCurrencyAmount().getCurrency();
    Currency counter = target.getProduct().getNearLeg().getCounterCurrencyAmount().getCurrency();
    CurrencyPair marketConventionPair = CurrencyPair.of(base, counter).toConventional();
    return Optional.of(marketConventionPair.getBase());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFxSwap product, RatesProvider provider);

}
