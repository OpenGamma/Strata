/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;

/**
 * A function group provides configuration for functions that perform calculations.
 * <p>
 * All functions in a group operate on the same type of calculation target.
 * <p>
 * Typically the functions in a group will be related in some way, for example they might all use the
 * same model to calculate their result.
 * 
 * @param <T>  the type of the calculation target
 */
public interface FunctionGroup<T extends CalculationTarget> {

  // TODO 2 methods
  //   boolean isFunctionAvailable(target, measure)
  //   SomeStructure functionConfig(target, measures) // N.B. not an optional - only invoke with supported measures

  // Would allow calc runner to figure out exactly which groups could calculate which measures, and then
  // the groups could provide functions that calculate one or more measures at the same time.

  /**
   * Returns configuration for a function to calculate the value of a measure for a target.
   * <p>
   * If this group has no function capable of calculating the value an empty optional is returned.
   *
   * @param target  the target of the calculation
   * @param measure  the measure that needs to be calculated
   * @return configuration for a function to calculate the value of a measure for a target if this group has one
   */
  Optional<FunctionConfig<T>> functionConfig(CalculationTarget target, Measure measure);

  /**
   * Returns the set of measures configured for a calculation target.
   * 
   * @param target  the calculation target
   * @return the set of measures configured for a calculation target
   */
  ImmutableSet<Measure> configuredMeasures(CalculationTarget target);

}
