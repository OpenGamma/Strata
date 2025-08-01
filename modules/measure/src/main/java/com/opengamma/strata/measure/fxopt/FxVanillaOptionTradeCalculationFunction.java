/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
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
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOptionTrade;

/**
 * Perform calculations on an FX vanilla option trade for each of a set of scenarios.
 * <p>
 * This uses Black FX option volatilities, which must be specified using {@link FxOptionMarketDataLookup}.
 * An instance of {@link RatesMarketDataLookup} must also be specified.
 * <p>
 * Two pricing methods are available, 'Black' and 'VannaVolga'.
 * By default, 'Black' will be used. To control the method, pass an instance of
 * {@link FxVanillaOptionMethod} in the calculation parameters.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum on rate curves}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed on rate curves}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum on rate curves}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed on rate curves}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#VEGA_MARKET_QUOTE_BUCKETED Vega market quote bucketed on volatility curves/surfaces}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * <p>
 * The "natural" currency is the market convention base currency of the underlying FX.
 */
public class FxVanillaOptionTradeCalculationFunction
    implements CalculationFunction<FxVanillaOptionTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, FxVanillaOptionMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, FxVanillaOptionMeasureCalculations.DEFAULT::pv01RatesCalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, FxVanillaOptionMeasureCalculations.DEFAULT::pv01RatesCalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, FxVanillaOptionMeasureCalculations.DEFAULT::pv01RatesMarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, FxVanillaOptionMeasureCalculations.DEFAULT::pv01RatesMarketQuoteBucketed)
          .put(Measures.CURRENCY_EXPOSURE, FxVanillaOptionMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.OPTION_DELTA, FxVanillaOptionMeasureCalculations.DEFAULT::delta)
          .put(Measures.CURRENT_CASH, FxVanillaOptionMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.VEGA_MARKET_QUOTE_BUCKETED, FxVanillaOptionMeasureCalculations.DEFAULT::vegaMarketQuoteBucketed)
          .put(Measures.RESOLVED_TARGET, (rt, smd, m, meth) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public FxVanillaOptionTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<FxVanillaOptionTrade> targetType() {
    return FxVanillaOptionTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(FxVanillaOptionTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(FxVanillaOptionTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrencyPair().getBase();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      FxVanillaOptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    FxVanillaOption product = trade.getProduct();
    CurrencyPair currencyPair = product.getCurrencyPair();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(
        ImmutableSet.of(currencyPair.getBase(), currencyPair.getCounter()));
    FxOptionMarketDataLookup optionLookup = parameters.getParameter(FxOptionMarketDataLookup.class);
    FunctionRequirements optionReqs = optionLookup.requirements(currencyPair);
    return ratesReqs.combinedWith(optionReqs);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      FxVanillaOptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // expand the trade once for all measures and all scenarios
    ResolvedFxVanillaOptionTrade resolved = trade.resolve(refData);
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData ratesMarketData = ratesLookup.marketDataView(scenarioMarketData);
    FxOptionMarketDataLookup optionLookup = parameters.getParameter(FxOptionMarketDataLookup.class);
    FxOptionScenarioMarketData optionMarketData = optionLookup.marketDataView(scenarioMarketData);
    FxVanillaOptionMethod method = parameters.findParameter(FxVanillaOptionMethod.class).orElse(FxVanillaOptionMethod.BLACK);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, ratesMarketData, optionMarketData, method));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for FxVanillaOptionTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, ratesMarketData, optionMarketData, method));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedFxVanillaOptionTrade trade,
        RatesScenarioMarketData ratesMarketData,
        FxOptionScenarioMarketData optionMarketData,
        FxVanillaOptionMethod method);
  }

}
