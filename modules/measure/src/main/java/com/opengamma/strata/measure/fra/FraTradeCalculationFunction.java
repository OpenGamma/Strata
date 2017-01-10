/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.AdvancedMeasures;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFraTrade;

/**
 * Perform calculations on a single {@code FraTrade} for each of a set of scenarios.
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
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 *   <li>{@linkplain AdvancedMeasures#PV01_SEMI_PARALLEL_GAMMA_BUCKETED PV01 semi-parallel gamma bucketed}
 *   <li>{@linkplain AdvancedMeasures#PV01_SINGLE_NODE_GAMMA_BUCKETED PV01 single node gamma bucketed}
 * </ul>
 */
public class FraTradeCalculationFunction
    implements CalculationFunction<FraTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, FraMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.EXPLAIN_PRESENT_VALUE, FraMeasureCalculations.DEFAULT::explainPresentValue)
          .put(Measures.PV01_CALIBRATED_SUM, FraMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, FraMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, FraMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, FraMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
          .put(Measures.PAR_RATE, FraMeasureCalculations.DEFAULT::parRate)
          .put(Measures.PAR_SPREAD, FraMeasureCalculations.DEFAULT::parSpread)
          .put(Measures.CASH_FLOWS, FraMeasureCalculations.DEFAULT::cashFlows)
          .put(Measures.CURRENCY_EXPOSURE, FraMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, FraMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.RESOLVED_TARGET, (rt, smd) -> rt)
          .put(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED, FraMeasureCalculations.DEFAULT::pv01SemiParallelGammaBucketed)
          .put(AdvancedMeasures.PV01_SINGLE_NODE_GAMMA_BUCKETED, FraMeasureCalculations.DEFAULT::pv01SingleNodeGammaBucketed)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public FraTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<FraTrade> targetType() {
    return FraTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(FraTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(FraTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      FraTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Fra product = trade.getProduct();
    Set<IborIndex> indices = new HashSet<>();
    indices.add(product.getIndex());
    product.getIndexInterpolated().ifPresent(indices::add);
    ImmutableSet<Currency> currencies = ImmutableSet.of(product.getCurrency());

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    return ratesLookup.requirements(currencies, indices);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      FraTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedFraTrade resolved = trade.resolve(refData);

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
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for FraTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedFraTrade trade,
        RatesScenarioMarketData marketData);
  }

}
