/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Function used when the target cannot be resolved.
 */
final class UnresolvableTargetCalculationFunction
    implements CalculationFunction<UnresolvableTarget> {

  /**
   * Shared instance.
   */
  static final CalculationFunction<UnresolvableTarget> INSTANCE = new UnresolvableTargetCalculationFunction();

  // restricted constructor
  private UnresolvableTargetCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<UnresolvableTarget> targetType() {
    return UnresolvableTarget.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    // pass all measures here so that the calculation is run to get the correct error message
    return ImmutableSet.copyOf(Measure.extendedEnum().lookupAllNormalized().values());
  }

  @Override
  public Currency naturalCurrency(UnresolvableTarget target, ReferenceData refData) {
    throw new IllegalStateException("Function has no currency-convertible measures");
  }

  @Override
  public FunctionRequirements requirements(
      UnresolvableTarget target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    return FunctionRequirements.empty();
  }

  @Override
  public Map<Measure, Result<?>> calculate(
      UnresolvableTarget target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    throw new IllegalStateException(Messages.format(
        "Target '{}' cannot be resolved: {}", target.getTarget().getClass(), target.getMessage()));
  }

}
