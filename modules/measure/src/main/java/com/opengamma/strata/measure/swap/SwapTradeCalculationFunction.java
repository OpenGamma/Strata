/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.AdvancedMeasures;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Perform calculations on a single {@code SwapTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * An instance of {@link RatesMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#EXPLAIN_PRESENT_VALUE Explain present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed}
 *   <li>{@linkplain Measures#PAR_RATE Par rate}
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#CASH_FLOWS Cash flows}
 *   <li>{@linkplain Measures#ACCRUED_INTEREST Accrued interest}
 *   <li>{@linkplain Measures#LEG_INITIAL_NOTIONAL Leg initial notional}
 *   <li>{@linkplain Measures#LEG_PRESENT_VALUE Leg present value}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 *   <li>{@linkplain AdvancedMeasures#PV01_SEMI_PARALLEL_GAMMA_BUCKETED PV01 semi-parallel gamma bucketed}
 * </ul>
 * <p>
 * The "natural" currency is the currency of the swaption, which is limited to be single-currency.
 */
public class SwapTradeCalculationFunction
    implements CalculationFunction<SwapTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, SwapMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.EXPLAIN_PRESENT_VALUE, SwapMeasureCalculations.DEFAULT::explainPresentValue)
          .put(Measures.PV01_CALIBRATED_SUM, SwapMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, SwapMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, SwapMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, SwapMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
          .put(Measures.PAR_RATE, SwapMeasureCalculations.DEFAULT::parRate)
          .put(Measures.PAR_SPREAD, SwapMeasureCalculations.DEFAULT::parSpread)
          .put(Measures.CASH_FLOWS, SwapMeasureCalculations.DEFAULT::cashFlows)
          .put(Measures.ACCRUED_INTEREST, SwapMeasureCalculations.DEFAULT::accruedInterest)
          .put(Measures.LEG_INITIAL_NOTIONAL, SwapMeasureCalculations.DEFAULT::legInitialNotional)
          .put(Measures.LEG_PRESENT_VALUE, SwapMeasureCalculations.DEFAULT::legPresentValue)
          .put(Measures.CURRENCY_EXPOSURE, SwapMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, SwapMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.RESOLVED_TARGET, (rt, smd) -> ScenarioArray.ofSingleValue(smd.getScenarioCount(), rt))
          .put(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED, SwapMeasureCalculations.DEFAULT::pv01SemiParallelGammaBucketed)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public SwapTradeCalculationFunction() {
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
  public Optional<String> identifier(SwapTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(SwapTrade trade, ReferenceData refData) {
    return trade.getProduct().getLegs().get(0).getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      SwapTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Swap product = trade.getProduct();
    ImmutableSet<Currency> currencies = product.allPaymentCurrencies();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    return ratesLookup.requirements(currencies, product.allIndices());
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      SwapTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedSwapTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData marketData = ratesLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, marketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedSwapTrade trade,
      RatesScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for SwapTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioArray<?> calculate(
        ResolvedSwapTrade trade,
        RatesScenarioMarketData marketData);
  }

}
