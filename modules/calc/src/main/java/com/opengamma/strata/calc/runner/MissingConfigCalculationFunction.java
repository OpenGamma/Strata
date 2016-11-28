/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Function used when there is no function registered that can calculate a requested value.
 */
final class MissingConfigCalculationFunction
    implements CalculationFunction<CalculationTarget> {

  /**
   * Shared instance.
   */
  static final CalculationFunction<CalculationTarget> INSTANCE = new MissingConfigCalculationFunction();

  // restricted constructor
  private MissingConfigCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<CalculationTarget> targetType() {
    return CalculationTarget.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return ImmutableSet.of();
  }

  @Override
  public Currency naturalCurrency(CalculationTarget trade, ReferenceData refData) {
    throw new IllegalStateException("Function has no currency-convertible measures");
  }

  @Override
  public FunctionRequirements requirements(
      CalculationTarget target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    return FunctionRequirements.empty();
  }

  @Override
  public Map<Measure, Result<?>> calculate(
      CalculationTarget target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    throw new IllegalStateException(Messages.format(
        "No function configured for measures {} on '{}'", measures, target.getClass().getSimpleName()));
  }

}
