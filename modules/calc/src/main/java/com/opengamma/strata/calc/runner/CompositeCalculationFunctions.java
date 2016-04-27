/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A set of calculation functions which combines the functions in two other sets of functions.
 */
class CompositeCalculationFunctions implements CalculationFunctions {

  /** The first set of calculation functions. */
  private final CalculationFunctions functions1;

  /** The second set of calculation functions. */
  private final CalculationFunctions functions2;

  /**
   * Creates an instance combining two sets of calculation functions.
   * <p>
   * If both sets of functions contain a function for a target the function from {@code functions1} is returned.
   *
   * @param functions1  the first set of functions
   * @param functions2  the second set of functions
   */
  CompositeCalculationFunctions(CalculationFunctions functions1, CalculationFunctions functions2) {
    this.functions1 = ArgChecker.notNull(functions1, "functions1");
    this.functions2 = ArgChecker.notNull(functions2, "functions2");
  }

  @Override
  public <T extends CalculationTarget> Optional<CalculationFunction<? super T>> findFunction(T target) {
    Optional<CalculationFunction<? super T>> function = functions1.findFunction(target);
    return function.isPresent() ? function : functions2.findFunction(target);
  }
}
