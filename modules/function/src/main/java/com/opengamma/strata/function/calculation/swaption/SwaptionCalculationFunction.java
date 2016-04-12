/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
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
import com.opengamma.strata.market.key.SwaptionVolatilitiesKey;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Perform calculations on a single {@code SwaptionTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 * </ul>
 * <p>
 * The "natural" currency is determined from the first swap leg.
 */
public class SwaptionCalculationFunction
    implements CalculationFunction<SwaptionTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, SwaptionMeasureCalculations::presentValue)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public SwaptionCalculationFunction() {
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
  public Currency naturalCurrency(SwaptionTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(SwaptionTrade trade, Set<Measure> measures, ReferenceData refData) {
    Swaption product = trade.getProduct();

    IborIndex index = product.getIndex();
    DiscountCurveKey dfKey = DiscountCurveKey.of(product.getCurrency());
    IborIndexCurveKey iborKey = IborIndexCurveKey.of(index);
    SwaptionVolatilitiesKey volKey = SwaptionVolatilitiesKey.of(index);
    IndexRateKey iborRateKey = IndexRateKey.of(index);

    return FunctionRequirements.builder()
        .singleValueRequirements(dfKey, iborKey, volKey)
        .timeSeriesRequirements(iborRateKey)
        .outputCurrencies(product.getCurrency())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      SwaptionTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData,
      ReferenceData refData) {

    // expand the trade once for all measures and all scenarios
    ResolvedSwaptionTrade resolved = trade.resolve(refData);
    IborIndex index = trade.getProduct().getIndex();
    SwaptionVolatilitiesKey volKey = SwaptionVolatilitiesKey.of(index);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, scenarioMarketData, volKey));
    }
    // The calculated value is the same for these two measures but they are handled differently WRT FX conversion
    FunctionUtils.duplicateResult(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY, results);
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedSwaptionTrade trade,
      CalculationMarketData scenarioMarketData,
      SwaptionVolatilitiesKey volKey) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, scenarioMarketData, volKey));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        ResolvedSwaptionTrade trade,
        CalculationMarketData marketData,
        SwaptionVolatilitiesKey volatilityKey);
  }

}
