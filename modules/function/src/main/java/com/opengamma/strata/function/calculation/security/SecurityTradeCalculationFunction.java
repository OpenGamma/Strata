/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
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
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityTrade;

/**
 * Perform calculations on a single {@code SecurityTrade} for each of a set of scenarios.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 * </ul>
 */
public class SecurityTradeCalculationFunction
    implements CalculationFunction<SecurityTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, SecurityMeasureCalculations::presentValue)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public SecurityTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<SecurityTrade> targetType() {
    return SecurityTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(SecurityTrade trade, ReferenceData refData) {
    Security security = refData.getValue(trade.getSecurityId());
    return security.getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      SecurityTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    Security security = refData.getValue(trade.getSecurityId());
    QuoteKey key = QuoteKey.of(trade.getSecurityId().getStandardId());

    return FunctionRequirements.builder()
        .singleValueRequirements(ImmutableSet.of(key))
        .outputCurrencies(security.getCurrency())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      SecurityTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      CalculationMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve security
    Security security = refData.getValue(trade.getSecurityId());

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, trade, security, scenarioMarketData));
    }
    // The calculated value is the same for these two measures but they are handled differently WRT FX conversion
    FunctionUtils.duplicateResult(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY, results);
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      SecurityTrade trade,
      Security security,
      CalculationMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(security, trade.getQuantity(), scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        Security security,
        double quantity,
        CalculationMarketData marketData);
  }

}
