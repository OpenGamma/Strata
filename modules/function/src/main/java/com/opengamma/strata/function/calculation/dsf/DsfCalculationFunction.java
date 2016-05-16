/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.dsf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.FunctionUtils;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.calculation.RatesMarketDataLookup;
import com.opengamma.strata.function.calculation.RatesScenarioMarketData;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.dsf.Dsf;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.dsf.ResolvedDsfTrade;

/**
 * Perform calculations on a single {@code DsfTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 *   <li>{@linkplain Measures#PV01 PV01}
 *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
 * </ul>
 * <p>
 * The "natural" currency is the currency of the swap leg that is received.
 */
public class DsfCalculationFunction
    implements CalculationFunction<DsfTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, DsfMeasureCalculations::presentValue)
          .put(Measures.PV01, DsfMeasureCalculations::pv01)
          .put(Measures.BUCKETED_PV01, DsfMeasureCalculations::bucketedPv01)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public DsfCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<DsfTrade> targetType() {
    return DsfTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(DsfTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      DsfTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Dsf product = trade.getProduct();
    QuoteKey quoteKey = QuoteKey.of(trade.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
    ImmutableSet<Index> indices = product.getUnderlyingSwap().allIndices();
    ImmutableSet<Currency> currencies = ImmutableSet.of(product.getCurrency());

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(currencies, indices);
    ImmutableSet<MarketDataKey<?>> valueReqs = ImmutableSet.<MarketDataKey<?>>builder()
        .add(quoteKey)
        .addAll(ratesReqs.getSingleValueRequirements())
        .build();
    return FunctionRequirements.builder()
        .singleValueRequirements(valueReqs)
        .timeSeriesRequirements(ratesReqs.getTimeSeriesRequirements())
        .outputCurrencies(currencies)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      DsfTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      CalculationMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedDsfTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData marketData = ratesLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, marketData));
    }
    // The calculated value is the same for these two measures but they are handled differently WRT FX conversion
    FunctionUtils.duplicateResult(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY, results);
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedDsfTrade trade,
      RatesScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        ResolvedDsfTrade trade,
        RatesScenarioMarketData marketData);
  }

}
