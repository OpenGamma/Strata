/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
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
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Perform calculations on a single {@code IborFutureTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 *   <li>{@linkplain Measures#PV01 PV01}
 *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
 * </ul>
 */
public class IborFutureCalculationFunction
    implements CalculationFunction<IborFutureTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PAR_SPREAD, IborFutureMeasureCalculations::parSpread)
          .put(Measures.PRESENT_VALUE, IborFutureMeasureCalculations::presentValue)
          .put(Measures.PV01, IborFutureMeasureCalculations::pv01)
          .put(Measures.BUCKETED_PV01, IborFutureMeasureCalculations::bucketedPv01)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public IborFutureCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(IborFutureTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(IborFutureTrade trade, Set<Measure> measures, ReferenceData refData) {
    IborFuture product = trade.getProduct();

    QuoteKey quoteKey = QuoteKey.of(trade.getProduct().getSecurityId().getStandardId());
    IborIndexCurveKey indexForwardCurveKey = IborIndexCurveKey.of(product.getIndex());
    DiscountCurveKey discountFactorsKey = DiscountCurveKey.of(product.getCurrency());
    IndexRateKey indexTimeSeriesKey = IndexRateKey.of(product.getIndex());

    return FunctionRequirements.builder()
        .singleValueRequirements(quoteKey, indexForwardCurveKey, discountFactorsKey)
        .timeSeriesRequirements(indexTimeSeriesKey)
        .outputCurrencies(product.getCurrency())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      IborFutureTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedIborFutureTrade resolved = trade.resolve(refData);

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
      ResolvedIborFutureTrade trade,
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
        ResolvedIborFutureTrade trade,
        CalculationMarketData marketData);
  }

}
