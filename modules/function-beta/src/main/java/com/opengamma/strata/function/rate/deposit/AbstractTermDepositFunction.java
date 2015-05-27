/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.deposit;

import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toScenarioResult;

import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.finance.rate.deposit.TermDeposit;
import com.opengamma.strata.finance.rate.deposit.TermDepositTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountingCurveKey;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.deposit.DiscountingTermDepositProductPricerBeta;

/**
 * Calculates a result for a {@code TermDepositTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractTermDepositFunction<T>
    implements CalculationSingleFunction<TermDepositTrade, ScenarioResult<T>> {

  /**
   * If this is true the value returned by the {@code execute} method will support automatic currency
   * conversion if the underlying results support it.
   */
  private final boolean convertCurrencies;

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractTermDepositFunction() {
    this(true);
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractTermDepositFunction(boolean convertCurrencies) {
    this.convertCurrencies = convertCurrencies;
  }

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
  public ScenarioResult<T> execute(TermDepositTrade trade, CalculationMarketData marketData) {
    ExpandedTermDeposit product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(convertCurrencies));
  }

  // execute for a single trade
  protected abstract T execute(ExpandedTermDeposit product, RatesProvider provider);

}
