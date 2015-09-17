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
import com.opengamma.strata.engine.calculation.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculation.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
import com.opengamma.strata.finance.fx.ExpandedFxNonDeliverableForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForwardTrade;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.fx.DiscountingFxNonDeliverableForwardProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Perform calculations on a single {@code FxNonDeliverableForwardTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined to be the settlement currency of the trade.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxNdfFunction<T>
    extends AbstractCalculationFunction<FxNonDeliverableForwardTrade, ScenarioResult<T>> {

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
  protected DiscountingFxNonDeliverableForwardProductPricer pricer() {
    return DiscountingFxNonDeliverableForwardProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(FxNonDeliverableForwardTrade trade) {
    FxNonDeliverableForward fx = trade.getProduct();
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
  public ScenarioResult<T> execute(FxNonDeliverableForwardTrade trade, CalculationMarketData marketData) {
    ExpandedFxNonDeliverableForward product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(FxNonDeliverableForwardTrade target) {
    return Optional.of(target.getProduct().getSettlementCurrency());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFxNonDeliverableForward product, RatesProvider provider);

}
