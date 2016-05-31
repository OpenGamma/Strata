/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Measures;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.calc.runner.FunctionUtils;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ScenarioResult;
import com.opengamma.strata.function.calculation.RatesMarketDataLookup;
import com.opengamma.strata.function.calculation.RatesScenarioMarketData;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.ResolvedFxSingleTrade;

/**
 * Perform calculations on a single {@code FxSingleTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 *   <li>{@linkplain Measures#PV01 PV01}
 *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#FORWARD_FX_RATE Forward FX rate}
 * </ul>
 * <p>
 * The "natural" currency is the base currency of the market convention pair of the two trade currencies.
 */
public class FxSingleCalculationFunction
    implements CalculationFunction<FxSingleTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PAR_SPREAD, FxSingleMeasureCalculations::parSpread)
          .put(Measures.PRESENT_VALUE, FxSingleMeasureCalculations::presentValue)
          .put(Measures.PV01, FxSingleMeasureCalculations::pv01)
          .put(Measures.BUCKETED_PV01, FxSingleMeasureCalculations::bucketedPv01)
          .put(Measures.CURRENCY_EXPOSURE, FxSingleMeasureCalculations::currencyExposure)
          .put(Measures.CURRENT_CASH, FxSingleMeasureCalculations::currentCash)
          .put(Measures.FORWARD_FX_RATE, FxSingleMeasureCalculations::forwardFxRate)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public FxSingleCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<FxSingleTrade> targetType() {
    return FxSingleTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(FxSingleTrade trade, ReferenceData refData) {
    Currency base = trade.getProduct().getBaseCurrencyAmount().getCurrency();
    Currency counter = trade.getProduct().getCounterCurrencyAmount().getCurrency();
    CurrencyPair marketConventionPair = CurrencyPair.of(base, counter).toConventional();
    return marketConventionPair.getBase();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      FxSingleTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    FxSingle fx = trade.getProduct();
    Currency baseCurrency = fx.getBaseCurrencyAmount().getCurrency();
    Currency counterCurrency = fx.getCounterCurrencyAmount().getCurrency();
    ImmutableSet<Currency> currencies = ImmutableSet.of(baseCurrency, counterCurrency);

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    return ratesLookup.requirements(currencies);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      FxSingleTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedFxSingleTrade resolved = trade.resolve(refData);

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
      ResolvedFxSingleTrade trade,
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
        ResolvedFxSingleTrade trade,
        RatesScenarioMarketData marketData);
  }

}
