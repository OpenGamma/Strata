/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Perform calculations on a single {@code IborFutureTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measure#PV01 PV01}
 *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
 * </ul>
 */
public class IborFutureCalculationFunction
    implements CalculationFunction<IborFutureTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measure.PAR_SPREAD, IborFutureMeasureCalculations::parSpread)
          .put(Measure.PRESENT_VALUE, IborFutureMeasureCalculations::presentValue)
          .put(Measure.PV01, IborFutureMeasureCalculations::pv01)
          .put(Measure.BUCKETED_PV01, IborFutureMeasureCalculations::bucketedPv01)
          .build();

  /**
   * Creates an instance.
   */
  public IborFutureCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<Measure> supportedMeasures() {
    return CALCULATORS.keySet();
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(IborFutureTrade target) {
    return Optional.of(target.getProduct().getCurrency());
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(IborFutureTrade trade, Set<Measure> measures) {
    IborFuture product = trade.getProduct();

    QuoteKey quoteKey = QuoteKey.of(trade.getSecurity().getStandardId());
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
      IborFutureTrade trade,
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
        IborFutureTrade trade,
        CalculationMarketData marketData);
  }

}
