/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.future;

import static com.opengamma.strata.engine.calculation.function.FunctionUtils.toScenarioResult;

import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.engine.calculation.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculation.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
import com.opengamma.strata.engine.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.finance.future.GenericFutureOptionTrade;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.market.key.QuoteKey;

/**
 * Perform calculations on a single {@code GenericFutureOptionTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractGenericFutureOptionFunction<T>
    extends AbstractCalculationFunction<GenericFutureOptionTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractGenericFutureOptionFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractGenericFutureOptionFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(GenericFutureOptionTrade trade) {
    QuoteKey key = QuoteKey.of(trade.getSecurity().getStandardId());
    return FunctionRequirements.builder()
        .singleValueRequirements(ImmutableSet.of(key))
        .outputCurrencies(trade.getProduct().getCurrency())
        .build();
  }

  @Override
  public ScenarioResult<T> execute(GenericFutureOptionTrade trade, CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(md -> execute(trade, md))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(GenericFutureOptionTrade target) {
    return Optional.of(target.getProduct().getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(GenericFutureOptionTrade trade, SingleCalculationMarketData marketData);

}
