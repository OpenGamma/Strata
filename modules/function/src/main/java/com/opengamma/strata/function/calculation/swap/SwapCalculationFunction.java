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
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Perform calculations on a single {@code SwapTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measure#PAR_RATE Par rate}
 *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measure#EXPLAIN_PRESENT_VALUE Explain present value}
 *   <li>{@linkplain Measure#CASH_FLOWS Cash flows}
 *   <li>{@linkplain Measure#PV01 PV01}
 *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
 *   <li>{@linkplain Measure#BUCKETED_GAMMA_PV01 Gamma PV01}
 *   <li>{@linkplain Measure#ACCRUED_INTEREST Accrued interest}
 *   <li>{@linkplain Measure#LEG_INITIAL_NOTIONAL Leg initial notional}
 *   <li>{@linkplain Measure#LEG_PRESENT_VALUE Leg present value}
 *   <li>{@linkplain Measure#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measure#CURRENT_CASH Current cash}
 * </ul>
 * <p>
 * The default reporting currency is determined from the first swap leg.
 */
public class SwapCalculationFunction
    implements CalculationFunction<SwapTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measure.PAR_RATE, SwapMeasureCalculations::parRate)
          .put(Measure.PAR_SPREAD, SwapMeasureCalculations::parSpread)
          .put(Measure.PRESENT_VALUE, SwapMeasureCalculations::presentValue)
          .put(Measure.EXPLAIN_PRESENT_VALUE, SwapMeasureCalculations::explainPresentValue)
          .put(Measure.CASH_FLOWS, SwapMeasureCalculations::cashFlows)
          .put(Measure.PV01, SwapMeasureCalculations::pv01)
          .put(Measure.BUCKETED_PV01, SwapMeasureCalculations::bucketedPv01)
          .put(Measure.BUCKETED_GAMMA_PV01, SwapMeasureCalculations::bucketedGammaPv01)
          .put(Measure.ACCRUED_INTEREST, SwapMeasureCalculations::accruedInterest)
          .put(Measure.LEG_INITIAL_NOTIONAL, SwapMeasureCalculations::legInitialNotional)
          .put(Measure.LEG_PRESENT_VALUE, SwapMeasureCalculations::legPresentValue)
          .put(Measure.CURRENCY_EXPOSURE, SwapMeasureCalculations::currencyExposure)
          .put(Measure.CURRENT_CASH, SwapMeasureCalculations::currentCash)
          .build();

  /**
   * Creates an instance.
   */
  public SwapCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<Measure> supportedMeasures() {
    return CALCULATORS.keySet();
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(SwapTrade target) {
    return Optional.of(target.getProduct().getLegs().get(0).getCurrency());
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(SwapTrade trade, Set<Measure> measures) {
    Swap product = trade.getProduct();

    // no market data for leg initial notional
    if (measures.equals(ImmutableSet.of(Measure.LEG_INITIAL_NOTIONAL))) {
      return FunctionRequirements.builder()
          .outputCurrencies(product.getLegs().stream().map(SwapLeg::getCurrency).collect(toImmutableSet()))
          .build();
    }

    // market data needed
    Set<Index> indices = product.allIndices();
    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());
    Set<MarketDataKey<?>> indexCurveKeys =
        indices.stream()
            .map(MarketDataKeys::indexCurve)
            .collect(toImmutableSet());
    Set<DiscountCurveKey> discountCurveKeys =
        product.getLegs().stream()
            .map(SwapLeg::getCurrency)
            .map(DiscountCurveKey::of)
            .collect(toImmutableSet());

    return FunctionRequirements.builder()
        .singleValueRequirements(Sets.union(indexCurveKeys, discountCurveKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(product.getLegs().stream().map(SwapLeg::getCurrency).collect(toImmutableSet()))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      SwapTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData) {

    // expand the trade once for all measures and all scenarios
    ExpandedSwap product = trade.getProduct().expand();

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, trade, product, scenarioMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      SwapTrade trade,
      ExpandedSwap product,
      CalculationMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, product, scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        SwapTrade trade,
        ExpandedSwap product,
        CalculationMarketData marketData);
  }

}
