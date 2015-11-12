/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import java.util.Map;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.collect.result.Result;

/**
 * A function that calculates multiple values for a target using multiple sets of market data.
 * <p>
 * If any of the calculated values contain any currency amounts and implement {@link CurrencyConvertible}
 * the calculation engine will automatically convert the amounts into the reporting currency.
 * <p>
 * If any of the calculated values contain currency amounts and the automatic currency conversion is
 * insufficient the function should implement {@link CurrencyAwareCalculationMultiFunction}.
 *
 * @param <T>  the type of target handled by this function
 */
public interface CalculationMultiFunction<T extends CalculationTarget>
    extends CalculationFunction<T> {

  // TODO Parameter of a set of measures in case the engine only requires a subset of what the function provides?
  /**
   * Calculates values of multiple measures for the target using multiple sets of market data.
   *
   * @param target  the target of the calculation
   * @param marketData  the market data used in the calculation
   * @return the calculated values, keyed by their measure
   */
  public abstract Map<Measure, Result<?>> execute(T target, CalculationMarketData marketData);
}
