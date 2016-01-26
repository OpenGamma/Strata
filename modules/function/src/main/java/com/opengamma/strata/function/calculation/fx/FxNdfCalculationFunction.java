/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.product.fx.ExpandedFxNdf;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;

/**
 * Perform calculations on a single {@code FxNdfTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measure#PV01 PV01}
 *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
 *   <li>{@linkplain Measure#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measure#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measure#FORWARD_FX_RATE Forward FX rate}
 * </ul>
 * <p>
 * The default reporting currency is the settlement currency of the trade.
 */
public class FxNdfCalculationFunction
    implements CalculationFunction<FxNdfTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measure.PRESENT_VALUE, FxNdfMeasureCalculations::presentValue)
          .put(Measure.PV01, FxNdfMeasureCalculations::pv01)
          .put(Measure.BUCKETED_PV01, FxNdfMeasureCalculations::bucketedPv01)
          .put(Measure.CURRENCY_EXPOSURE, FxNdfMeasureCalculations::currencyExposure)
          .put(Measure.CURRENT_CASH, FxNdfMeasureCalculations::currentCash)
          .put(Measure.FORWARD_FX_RATE, FxNdfMeasureCalculations::forwardFxRate)
          .build();

  /**
   * Creates an instance.
   */
  public FxNdfCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<Measure> supportedMeasures() {
    return CALCULATORS.keySet();
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(FxNdfTrade target) {
    return Optional.of(target.getProduct().getSettlementCurrency());
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(FxNdfTrade trade, Set<Measure> measures) {
    FxNdf fx = trade.getProduct();
    Currency settleCurrency = fx.getSettlementCurrency();
    Currency otherCurrency = fx.getNonDeliverableCurrency();

    Set<DiscountCurveKey> discountCurveKeys =
        ImmutableSet.of(DiscountCurveKey.of(settleCurrency), DiscountCurveKey.of(otherCurrency));

    return FunctionRequirements.builder()
        .singleValueRequirements(discountCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(settleCurrency, otherCurrency)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      FxNdfTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData) {

    // expand the trade once for all measures and all scenarios
    ExpandedFxNdf product = trade.getProduct().expand();

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, trade, product, scenarioMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, product, scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        FxNdfTrade trade,
        ExpandedFxNdf product,
        CalculationMarketData marketData);
  }

}
