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
import com.opengamma.strata.finance.fx.ExpandedFx;
import com.opengamma.strata.finance.fx.FxForward;
import com.opengamma.strata.finance.fx.FxForwardTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountingCurveKey;
import com.opengamma.strata.pricer.fx.DiscountingFxProductPricerBeta;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates a result for an {@code FxForwardTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxForwardFunction<T>
    implements CalculationSingleFunction<FxForwardTrade, List<T>> {

  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxProductPricerBeta pricer() {
    return DiscountingFxProductPricerBeta.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRequirements requirements(FxForwardTrade trade) {
    FxForward fx = trade.getProduct();
    Currency baseCurrency = fx.getBaseCurrencyAmount().getCurrency();
    Currency counterCurrency = fx.getCounterCurrencyAmount().getCurrency();

    Set<DiscountingCurveKey> discountingCurveKeys = ImmutableSet.of(
        DiscountingCurveKey.of(baseCurrency), DiscountingCurveKey.of(counterCurrency));

    return CalculationRequirements.builder()
        .singleValueRequirements(discountingCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(baseCurrency, counterCurrency)
        .build();
  }

  @Override
  public List<T> execute(FxForwardTrade trade, CalculationMarketData marketData) {
    ExpandedFx product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toList());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFx product, RatesProvider provider);

}
