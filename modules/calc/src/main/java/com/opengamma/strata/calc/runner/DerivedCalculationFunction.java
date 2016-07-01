/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Map;
import java.util.Set;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * A derived calculation function calculates one measure using the measures calculated by another function.
 * <p>
 * Strata executes the other function and checks that all required measures are available before calling
 * this function.
 * <p>
 * A derived calculation function can be added to an existing set of calculation functions using
 * {@link CalculationFunctions#composedWith(DerivedCalculationFunction[])}.
 *
 * @param <T> the type of the target handled by this function, often a trade
 * @param <R> the type of value calculated by this function
 */
public interface DerivedCalculationFunction<T extends CalculationTarget, R> {

  /**
   * Returns the type of calculation target handled by the function.
   *
   * @return the type of calculation target handled by the function
   */
  public abstract Class<T> targetType();

  /**
   * Returns the measure calculated by the function.
   *
   * @return the measure calculated by the function
   */
  public abstract Measure measure();

  /**
   * Returns the measures required by this function to calculate its measure.
   *
   * @return the measures required by this function to calculate its measure
   */
  public abstract Set<Measure> requiredMeasures();

  /**
   * Returns requirements for the market data required by this function to calculate its measure.
   *
   * @param target  the target of the calculation, often a trade
   * @param parameters  the calculation parameters specifying how the calculations should be performed
   * @param refData  the reference data used in the calculations
   * @return requirements for the market data required by this function to calculate its measure
   */
  public abstract FunctionRequirements requirements(T target, CalculationParameters parameters, ReferenceData refData);

  /**
   * Calculates the measure.
   * <p>
   * This method is only invoked if all of the required measures are available.
   * Therefore implementation can safely assume that {@code requiredMeasures} contains all the
   * required data.
   *
   * @param target  the target of the calculation, often a trade
   * @param requiredMeasures  the calculated measure values required by this function to calculate its measure
   * @param parameters  the calculation parameters specifying how the calculations should be performed
   * @param marketData  the market data used in the calculations
   * @param refData  the reference data used in the calculations
   * @return the calculated measure value.
   */
  public abstract R calculate(
      T target,
      Map<Measure, Object> requiredMeasures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData);
}
