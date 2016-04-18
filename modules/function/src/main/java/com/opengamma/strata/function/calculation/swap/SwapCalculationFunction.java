/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.FunctionUtils;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Perform calculations on a single {@code SwapTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PAR_RATE Par rate}
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 *   <li>{@linkplain Measures#EXPLAIN_PRESENT_VALUE Explain present value}
 *   <li>{@linkplain Measures#CASH_FLOWS Cash flows}
 *   <li>{@linkplain Measures#PV01 PV01}
 *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
 *   <li>{@linkplain Measures#BUCKETED_GAMMA_PV01 Gamma PV01}
 *   <li>{@linkplain Measures#ACCRUED_INTEREST Accrued interest}
 *   <li>{@linkplain Measures#LEG_INITIAL_NOTIONAL Leg initial notional}
 *   <li>{@linkplain Measures#LEG_PRESENT_VALUE Leg present value}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 * </ul>
 * <p>
 * The "natural" currency is the currency of the swaption, which is limited to be single-currency.
 */
public class SwapCalculationFunction
    implements CalculationFunction<SwapTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PAR_RATE, SwapMeasureCalculations::parRate)
          .put(Measures.PAR_SPREAD, SwapMeasureCalculations::parSpread)
          .put(Measures.PRESENT_VALUE, SwapMeasureCalculations::presentValue)
          .put(Measures.EXPLAIN_PRESENT_VALUE, SwapMeasureCalculations::explainPresentValue)
          .put(Measures.CASH_FLOWS, SwapMeasureCalculations::cashFlows)
          .put(Measures.PV01, SwapMeasureCalculations::pv01)
          .put(Measures.BUCKETED_PV01, SwapMeasureCalculations::bucketedPv01)
          .put(Measures.BUCKETED_GAMMA_PV01, SwapMeasureCalculations::bucketedGammaPv01)
          .put(Measures.ACCRUED_INTEREST, SwapMeasureCalculations::accruedInterest)
          .put(Measures.LEG_INITIAL_NOTIONAL, SwapMeasureCalculations::legInitialNotional)
          .put(Measures.LEG_PRESENT_VALUE, SwapMeasureCalculations::legPresentValue)
          .put(Measures.CURRENCY_EXPOSURE, SwapMeasureCalculations::currencyExposure)
          .put(Measures.CURRENT_CASH, SwapMeasureCalculations::currentCash)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public SwapCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<SwapTrade> targetType() {
    return SwapTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(SwapTrade trade, ReferenceData refData) {
    return trade.getProduct().getLegs().get(0).getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(SwapTrade trade, Set<Measure> measures, ReferenceData refData) {
    Swap product = trade.getProduct();

    // no market data for leg initial notional
    if (measures.equals(ImmutableSet.of(Measures.LEG_INITIAL_NOTIONAL))) {
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
      CalculationMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedSwapTrade resolved = trade.resolve(refData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, scenarioMarketData));
    }
    // The calculated value is the same for these two measures but they are handled differently WRT FX conversion
    FunctionUtils.duplicateResult(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY, results);
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedSwapTrade trade,
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
        ResolvedSwapTrade trade,
        CalculationMarketData marketData);
  }

}
