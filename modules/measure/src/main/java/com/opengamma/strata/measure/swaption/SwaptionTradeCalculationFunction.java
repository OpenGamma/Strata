/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Perform calculations on a single {@code SwaptionTrade} for each of a set of scenarios.
 * <p>
 * This uses Black, Normal or SABR swaption volatilities,
 * which must be specified using {@link SwaptionMarketDataLookup}.
 * An instance of {@link RatesMarketDataLookup} must also be specified.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum on rate curves}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed on rate curves}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum on rate curves}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed on rate curves}
 *   <li>{@linkplain Measures#VEGA_MARKET_QUOTE_BUCKETED normal vega market quote bucketed on vol curves/surfaces}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * <p>
 * The "natural" currency is determined from the first swap leg.
 */
public class SwaptionTradeCalculationFunction
    implements CalculationFunction<SwaptionTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, SwaptionMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, SwaptionMeasureCalculations.DEFAULT::pv01RatesCalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, SwaptionMeasureCalculations.DEFAULT::pv01RatesCalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, SwaptionMeasureCalculations.DEFAULT::pv01RatesMarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, SwaptionMeasureCalculations.DEFAULT::pv01RatesMarketQuoteBucketed)
          .put(Measures.VEGA_MARKET_QUOTE_BUCKETED, SwaptionMeasureCalculations.DEFAULT::vegaMarketQuoteBucketed)
          .put(Measures.CURRENCY_EXPOSURE, SwaptionMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, SwaptionMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.RESOLVED_TARGET, (rt, smd, m) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public SwaptionTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<SwaptionTrade> targetType() {
    return SwaptionTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(SwaptionTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(SwaptionTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      SwaptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Swaption product = trade.getProduct();
    Currency currency = product.getCurrency();
    RateIndex index = product.getIndex();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(currency, index);
    SwaptionMarketDataLookup swaptionLookup = parameters.getParameter(SwaptionMarketDataLookup.class);
    FunctionRequirements swaptionReqs = swaptionLookup.requirements(index);
    return ratesReqs.combinedWith(swaptionReqs);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      SwaptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // expand the trade once for all measures and all scenarios
    ResolvedSwaptionTrade resolved = trade.resolve(refData);
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData ratesMarketData = ratesLookup.marketDataView(scenarioMarketData);
    SwaptionMarketDataLookup swaptionLookup = parameters.getParameter(SwaptionMarketDataLookup.class);
    SwaptionScenarioMarketData swaptionMarketData = swaptionLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, ratesMarketData, swaptionMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for SwaptionTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, ratesMarketData, swaptionMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedSwaptionTrade trade,
        RatesScenarioMarketData ratesMarketData,
        SwaptionScenarioMarketData swaptionMarketData);
  }

}
