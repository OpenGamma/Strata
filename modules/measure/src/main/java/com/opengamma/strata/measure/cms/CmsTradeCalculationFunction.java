/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
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
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;
import com.opengamma.strata.measure.swaption.SwaptionScenarioMarketData;
import com.opengamma.strata.product.cms.Cms;
import com.opengamma.strata.product.cms.CmsTrade;
import com.opengamma.strata.product.cms.ResolvedCmsTrade;

/**
 * Perform calculations on a single {@code CmsTrade} for each of a set of scenarios.
 * <p>
 * This uses SABR swaption volatilities, which must be specified using {@link SwaptionMarketDataLookup}.
 * An instance of {@link RatesMarketDataLookup} and {@link CmsSabrExtrapolationParams} must also be specified.
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
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * <p>
 * The "natural" currency is determined from the CMS leg.
 */
public class CmsTradeCalculationFunction
    implements CalculationFunction<CmsTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, CmsMeasureCalculations::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, CmsMeasureCalculations::pv01RatesCalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, CmsMeasureCalculations::pv01RatesCalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, CmsMeasureCalculations::pv01RatesMarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, CmsMeasureCalculations::pv01RatesMarketQuoteBucketed)
          .put(Measures.CURRENCY_EXPOSURE, CmsMeasureCalculations::currencyExposure)
          .put(Measures.CURRENT_CASH, CmsMeasureCalculations::currentCash)
          .put(Measures.RESOLVED_TARGET, (c, rt, smd, m) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public CmsTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<CmsTrade> targetType() {
    return CmsTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(CmsTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(CmsTrade trade, ReferenceData refData) {
    return trade.getProduct().getCmsLeg().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      CmsTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Cms product = trade.getProduct();
    Set<Currency> currencies = product.allPaymentCurrencies();
    IborIndex cmsIndex = trade.getProduct().getCmsLeg().getUnderlyingIndex();
    Set<Index> payIndices = trade.getProduct().allRateIndices();
    Set<Index> indices = ImmutableSet.<Index>builder().add(cmsIndex).addAll(payIndices).build();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(currencies, indices);
    SwaptionMarketDataLookup swaptionLookup = parameters.getParameter(SwaptionMarketDataLookup.class);
    FunctionRequirements swaptionReqs = swaptionLookup.requirements(cmsIndex);
    return ratesReqs.combinedWith(swaptionReqs);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      CmsTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // expand the trade once for all measures and all scenarios
    ResolvedCmsTrade resolved = trade.resolve(refData);
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData ratesMarketData = ratesLookup.marketDataView(scenarioMarketData);
    SwaptionMarketDataLookup swaptionLookup = parameters.getParameter(SwaptionMarketDataLookup.class);
    SwaptionScenarioMarketData swaptionMarketData = swaptionLookup.marketDataView(scenarioMarketData);
    CmsSabrExtrapolationParams cmsParams = parameters.getParameter(CmsSabrExtrapolationParams.class);
    CmsMeasureCalculations calculations = new CmsMeasureCalculations(cmsParams);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, calculations, ratesMarketData, swaptionMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedCmsTrade trade,
      CmsMeasureCalculations calculations,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for SwaptionTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(calculations, trade, ratesMarketData, swaptionMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        CmsMeasureCalculations calculations,
        ResolvedCmsTrade trade,
        RatesScenarioMarketData ratesMarketData,
        SwaptionScenarioMarketData swaptionMarketData);
  }

}
