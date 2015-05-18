/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.fx.ExpandedFxNonDeliverableForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForwardTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountingCurveKey;
import com.opengamma.strata.pricer.fx.DiscountingFxNonDeliverableForwardProductPricerBeta;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates a result for an {@code FxNonDeliverableForwardTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxNonDeliverableForwardFunction<T>
    implements CalculationSingleFunction<FxNonDeliverableForwardTrade, List<T>> {

  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxNonDeliverableForwardProductPricerBeta pricer() {
    return DiscountingFxNonDeliverableForwardProductPricerBeta.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRequirements requirements(FxNonDeliverableForwardTrade trade) {
    FxNonDeliverableForward fx = trade.getProduct();
    Currency settleCurrency = fx.getSettlementCurrency();
    Currency otherCurrency = fx.getNonDeliverableCurrency();

    Set<DiscountingCurveKey> discountingCurveKeys = ImmutableSet.of(
        DiscountingCurveKey.of(settleCurrency), DiscountingCurveKey.of(otherCurrency));

    return CalculationRequirements.builder()
        .singleValueRequirements(discountingCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(settleCurrency, otherCurrency)
        .build();
  }

  @Override
  public List<T> execute(FxNonDeliverableForwardTrade trade, CalculationMarketData marketData) {
    ExpandedFxNonDeliverableForward product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toList());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFxNonDeliverableForward product, RatesProvider provider);

}
