/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.deposit;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.finance.rate.deposit.TermDeposit;
import com.opengamma.strata.finance.rate.deposit.TermDepositTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.deposit.DiscountingTermDepositProductPricerBeta;

/**
 * Calculates a result for a {@code TermDepositTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractTermDepositFunction<T>
    implements EngineSingleFunction<TermDepositTrade, List<T>> {

  /**
   * Returns the Term Deposit pricer.
   * 
   * @return the pricer
   */
  protected DiscountingTermDepositProductPricerBeta pricer() {
    return DiscountingTermDepositProductPricerBeta.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRequirements requirements(TermDepositTrade trade) {
    TermDeposit deposit = trade.getProduct();

    Set<DiscountingCurveKey> discountingCurveKeys = ImmutableSet.of(DiscountingCurveKey.of(deposit.getCurrency()));

    return CalculationRequirements.builder()
        .singleValueRequirements(discountingCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(deposit.getCurrency())
        .build();
  }

  @Override
  public List<T> execute(TermDepositTrade trade, CalculationMarketData marketData) {
    ExpandedTermDeposit product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toList());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedTermDeposit product, RatesProvider provider);

}
