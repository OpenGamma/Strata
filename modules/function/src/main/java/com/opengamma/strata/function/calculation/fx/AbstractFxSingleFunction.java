/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxSingle;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * Perform calculations on a single {@code FxSingleTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined to be the base currency of the market convention
 * pair of the two trade currencies.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxSingleFunction<T>
    extends AbstractCalculationFunction<FxSingleTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractFxSingleFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractFxSingleFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxSingleProductPricer pricer() {
    return DiscountingFxSingleProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(FxSingleTrade trade) {
    FxSingle fx = trade.getProduct();
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
  public ScenarioResult<T> execute(FxSingleTrade trade, CalculationMarketData marketData) {
    ExpandedFxSingle product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(FxSingleTrade target) {
    Currency base = target.getProduct().getBaseCurrencyAmount().getCurrency();
    Currency counter = target.getProduct().getCounterCurrencyAmount().getCurrency();
    CurrencyPair marketConventionPair = CurrencyPair.of(base, counter).toConventional();
    return Optional.of(marketConventionPair.getBase());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFxSingle product, RatesProvider provider);

}
