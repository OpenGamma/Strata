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
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.VectorEngineFunction;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.fx.FxExchange;
import com.opengamma.strata.finance.fx.FxExchangeTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.pricer.fx.DiscountingFxExchangeProductPricerBeta;

/**
 * Calculates a result for an {@code FxExchange} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFxExchangeFunction<T>
    implements VectorEngineFunction<FxExchangeTrade, List<T>> {

  // Pricer
  private static final DiscountingFxExchangeProductPricerBeta PRICER = DiscountingFxExchangeProductPricerBeta.DEFAULT;

  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFxExchangeProductPricerBeta pricer() {
    return PRICER;
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRequirements requirements(FxExchangeTrade trade) {
    FxExchange fx = trade.getProduct();
    Currency baseCurrency = fx.getBaseCurrencyPayment().getCurrency();
    Currency counterCurrency = fx.getCounterCurrencyPayment().getCurrency();

    Set<DiscountingCurveKey> discountingCurveKeys = ImmutableSet.of(
        DiscountingCurveKey.of(baseCurrency), DiscountingCurveKey.of(counterCurrency));

    return CalculationRequirements.builder()
        .singleValueRequirements(discountingCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(baseCurrency, counterCurrency)
        .build();
  }

  @Override
  public List<T> execute(
      FxExchangeTrade trade,
      CalculationMarketData marketData,
      ReportingRules reportingRules) {

    FxExchange product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toList());
  }

  // execute for a single trade
  protected abstract T execute(FxExchange product, MarketDataRatesProvider provider);

}
