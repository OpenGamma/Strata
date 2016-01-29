/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.swap.DeliverableSwapFuture;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;

/**
 * Perform calculations on a single {@code DeliverableSwapFutureTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01 PV01}
 *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
 * </ul>
 * <p>
 * The "natural" currency is the currency of the swap leg that is received.
 */
public class DeliverableSwapFutureCalculationFunction
    implements CalculationFunction<DeliverableSwapFutureTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, DeliverableSwapFutureMeasureCalculations::presentValue)
          .put(Measures.PV01, DeliverableSwapFutureMeasureCalculations::pv01)
          .put(Measures.BUCKETED_PV01, DeliverableSwapFutureMeasureCalculations::bucketedPv01)
          .build();

  /**
   * Creates an instance.
   */
  public DeliverableSwapFutureCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<Measure> supportedMeasures() {
    return CALCULATORS.keySet();
  }

  @Override
  public Optional<Currency> naturalCurrency(DeliverableSwapFutureTrade target) {
    return Optional.of(target.getProduct().getCurrency());
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(DeliverableSwapFutureTrade trade, Set<Measure> measures) {
    DeliverableSwapFuture product = trade.getProduct();
    QuoteKey quoteKey = QuoteKey.of(trade.getSecurity().getStandardId());
    Set<Index> indices = product.getUnderlyingSwap().allIndices();
    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());
    Set<MarketDataKey<?>> indexCurveKeys =
        indices.stream()
            .map(MarketDataKeys::indexCurve)
            .collect(toImmutableSet());
    DiscountCurveKey discountFactorsKey = DiscountCurveKey.of(product.getCurrency());
    Set<MarketDataKey<?>> reqs = ImmutableSet.<MarketDataKey<?>>builder()
        .add(quoteKey)
        .add(discountFactorsKey)
        .addAll(indexCurveKeys)
        .build();

    return FunctionRequirements.builder()
        .singleValueRequirements(reqs)
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(product.getCurrency())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      DeliverableSwapFutureTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData) {

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, trade, scenarioMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      DeliverableSwapFutureTrade trade,
      CalculationMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        DeliverableSwapFutureTrade trade,
        CalculationMarketData marketData);
  }

}
