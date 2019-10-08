/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.CalculationTarget;

/**
 * A set of {@link DerivedCalculationFunction} instances which decorates another {@link CalculationFunctions}
 * instance, creating a combined set of functions.
 */
class DerivedCalculationFunctions implements CalculationFunctions {

  /** The underlying set of calculation functions. */
  private final CalculationFunctions delegateFunctions;

  /** Derived calculation functions keyed by the type of target they handle. */
  private final ListMultimap<Class<?>, DerivedCalculationFunction<?, ?>> functionsByTargetType;

  /**
   * Creates an instance.
   *
   * @param delegateFunctions  the underlying set of calculation functions
   * @param functions  the derived calculation functions
   */
  DerivedCalculationFunctions(
      CalculationFunctions delegateFunctions,
      List<DerivedCalculationFunction<?, ?>> functions) {

    this.delegateFunctions = delegateFunctions;
    // need to preserve order, and generics are complex, so avoid streams
    ListMultimap<Class<?>, DerivedCalculationFunction<?, ?>> listMultimap = ArrayListMultimap.create();
    for (DerivedCalculationFunction<?, ?> fn : functions) {
      listMultimap.put(fn.targetType(), fn);
    }
    this.functionsByTargetType = listMultimap;
  }

  @Override
  public <T extends CalculationTarget> Optional<CalculationFunction<? super T>> findFunction(T target) {
    return delegateFunctions.findFunction(target).map(fn -> wrap(fn, target));
  }

  @SuppressWarnings("unchecked")
  private <T extends CalculationTarget, R> CalculationFunction<? super T> wrap(
      CalculationFunction<? super T> fn,
      T target) {

    List<DerivedCalculationFunction<?, ?>> derivedFunctions = functionsByTargetType.get(target.getClass());
    if (derivedFunctions == null) {
      return fn;
    }
    CalculationFunction<? super T> wrappedFn = fn;
    for (DerivedCalculationFunction<?, ?> derivedFn : derivedFunctions) {
      // These casts are necessary because the type information is lost when the functions are stored in the map.
      // They are safe because T is the target type which is the map key and R isn't actually used
      CalculationFunction<T> wrappedFnCast = (CalculationFunction<T>) wrappedFn;
      DerivedCalculationFunction<T, R> derivedFnCast = (DerivedCalculationFunction<T, R>) derivedFn;
      wrappedFn = new DerivedCalculationFunctionWrapper<>(derivedFnCast, wrappedFnCast);
    }
    return wrappedFn;
  }
}
