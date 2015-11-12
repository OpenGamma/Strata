/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.deposit;

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
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.product.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.product.rate.deposit.TermDeposit;
import com.opengamma.strata.product.rate.deposit.TermDepositTrade;

/**
 * Perform calculations on a single {@code TermDepositTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractTermDepositFunction<T>
    extends AbstractCalculationFunction<TermDepositTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractTermDepositFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractTermDepositFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingTermDepositProductPricer pricer() {
    return DiscountingTermDepositProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(TermDepositTrade trade) {
    TermDeposit deposit = trade.getProduct();

    Set<DiscountFactorsKey> discountCurveKeys =
        ImmutableSet.of(DiscountFactorsKey.of(deposit.getCurrency()));

    return FunctionRequirements.builder()
        .singleValueRequirements(discountCurveKeys)
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
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(TermDepositTrade target) {
    return Optional.of(target.getProduct().getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedTermDeposit product, RatesProvider provider);

}
