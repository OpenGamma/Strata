/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.payment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.calc.runner.FunctionUtils;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Perform calculations on a single {@code BulletPaymentTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 * </ul>
 */
public class BulletPaymentTradeCalculationFunction
    implements CalculationFunction<BulletPaymentTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, BulletPaymentMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, BulletPaymentMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, BulletPaymentMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, BulletPaymentMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, BulletPaymentMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
          .put(Measures.CURRENCY_EXPOSURE, BulletPaymentMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, BulletPaymentMeasureCalculations.DEFAULT::currentCash)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public BulletPaymentTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<BulletPaymentTrade> targetType() {
    return BulletPaymentTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(BulletPaymentTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      BulletPaymentTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    BulletPayment product = trade.getProduct();
    Currency currency = product.getCurrency();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    return ratesLookup.requirements(currency);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      BulletPaymentTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedBulletPaymentTrade resolved = trade.resolve(refData);

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
      ResolvedBulletPaymentTrade trade,
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
    public abstract ScenarioArray<?> calculate(
        ResolvedBulletPaymentTrade trade,
        RatesScenarioMarketData marketData);
  }

}
