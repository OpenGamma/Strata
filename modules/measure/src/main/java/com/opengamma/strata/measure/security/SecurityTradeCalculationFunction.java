/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.security;

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
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityTrade;

/**
 * Perform calculations on a single {@code SecurityTrade} for each of a set of scenarios.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
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

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

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
  public Optional<String> identifier(SecurityTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
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
    QuoteId id = QuoteId.of(trade.getSecurityId().getStandardId());

    return FunctionRequirements.builder()
        .valueRequirements(ImmutableSet.of(id))
        .outputCurrencies(security.getCurrency())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      SecurityTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve security
    Security security = refData.getValue(trade.getSecurityId());

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, trade, security, scenarioMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      SecurityTrade trade,
      Security security,
      ScenarioMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for SecurityTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(security, trade.getQuantity(), scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioArray<?> calculate(
        Security security,
        double quantity,
        ScenarioMarketData marketData);
  }

}
