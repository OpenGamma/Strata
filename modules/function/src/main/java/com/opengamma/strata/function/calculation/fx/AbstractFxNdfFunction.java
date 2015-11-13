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
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.fx.DiscountingFxNdfProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxNdf;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;

/**
 * Perform calculations on a single {@code FxNdfTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined to be the settlement currency of the trade.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxNdfFunction<T>
    extends AbstractCalculationFunction<FxNdfTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractFxNdfFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractFxNdfFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxNdfProductPricer pricer() {
    return DiscountingFxNdfProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(FxNdfTrade trade) {
    FxNdf fx = trade.getProduct();
    Currency settleCurrency = fx.getSettlementCurrency();
    Currency otherCurrency = fx.getNonDeliverableCurrency();

    Set<DiscountFactorsKey> discountCurveKeys =
        ImmutableSet.of(DiscountFactorsKey.of(settleCurrency), DiscountFactorsKey.of(otherCurrency));

    return FunctionRequirements.builder()
        .singleValueRequirements(discountCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(settleCurrency, otherCurrency)
        .build();
  }

  @Override
  public ScenarioResult<T> execute(FxNdfTrade trade, CalculationMarketData marketData) {
    ExpandedFxNdf product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(FxNdfTrade target) {
    return Optional.of(target.getProduct().getSettlementCurrency());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFxNdf product, RatesProvider provider);

}
