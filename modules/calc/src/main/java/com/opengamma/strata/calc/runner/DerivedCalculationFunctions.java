/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static java.util.stream.Collectors.groupingBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;

/**
 * A set of {@link DerivedCalculationFunction} instances which decorates another {@link CalculationFunctions}
 * instance, creating a combined set of functions.
 */
class DerivedCalculationFunctions implements CalculationFunctions {

  /** The underlying set of calculation functions. */
  private final CalculationFunctions delegateFunctions;

  /** Derived calculation functions keyed by the type of target they handle. */
  private final Map<Class<?>, List<DerivedCalculationFunction<?, ?>>> functionsByTargetType;

  /**
   * Creates an instance.
   *
   * @param delegateFunctions  the underlying set of calculation functions
   * @param functions  the derived calculation functions
   */
  DerivedCalculationFunctions(CalculationFunctions delegateFunctions, DerivedCalculationFunction<?, ?>... functions) {
    this.delegateFunctions = delegateFunctions;
    this.functionsByTargetType = Arrays.stream(functions).collect(groupingBy(fn -> fn.targetType()));
  }

  @Override
  public <T extends CalculationTarget> Optional<CalculationFunction<? super T>> findFunction(T target) {
    return delegateFunctions.findFunction(target).map(fn -> wrap(fn, target));
  }

  @SuppressWarnings("unchecked")
  private <T extends CalculationTarget, R> CalculationFunction<? super T> wrap(CalculationFunction<? super T> fn, T target) {
    List<DerivedCalculationFunction<?, ?>> derivedFunctions = functionsByTargetType.get(target.getClass());
    CalculationFunction<? super T> wrappedFn = fn;

    for (DerivedCalculationFunction<?, ?> derivedFn : derivedFunctions) {
      // These casts are necessary because the type information is lost when the functions are stored in the map.
      // They are safe because T is the target type which is is the map key and R isn't actually used
      CalculationFunction<T> wrappedFnCast = (CalculationFunction<T>) wrappedFn;
      DerivedCalculationFunction<T, R> derivedFnCast = (DerivedCalculationFunction<T, R>) derivedFn;
      wrappedFn = new DerivedCalculationFunctionWrapper<>(derivedFnCast, wrappedFnCast);
    }
    return wrappedFn;
  }
}
