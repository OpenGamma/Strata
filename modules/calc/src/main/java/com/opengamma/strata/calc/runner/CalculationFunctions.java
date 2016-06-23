/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.opengamma.strata.basics.CalculationTarget;

/**
 * The calculation functions.
 * <p>
 * This provides the complete set of functions that will be used in a calculation.
 * <p>
 * The default implementation is accessed by the static factory methods.
 * It matches the {@link CalculationFunction} by the type of the {@link CalculationTarget}.
 * As such, the default implementation is essentially a {@code Map} where the keys are the
 * target type {@code Class} that the function operates on.
 */
public interface CalculationFunctions {

  /**
   * Obtains an empty instance with no functions.
   * 
   * @return the empty instance
   */
  public static CalculationFunctions empty() {
    return DefaultCalculationFunctions.EMPTY;
  }

  /**
   * Obtains an instance from the specified functions.
   * <p>
   * This returns an implementation that matches the function by the type of the
   * target, as returned by {@link CalculationFunction#targetType()}.
   * The list will be converted to a {@code Map} keyed by the target type.
   * Each function must refer to a different target type.
   * 
   * @param functions  the functions
   * @return the calculation functions
   */
  public static CalculationFunctions of(CalculationFunction<?>... functions) {
    return DefaultCalculationFunctions.of(Stream.of(functions).collect(toImmutableMap(fn -> fn.targetType())));
  }

  /**
   * Obtains an instance from the specified functions.
   * <p>
   * This returns an implementation that matches the function by the type of the
   * target, as returned by {@link CalculationFunction#targetType()}.
   * The list will be converted to a {@code Map} keyed by the target type.
   * Each function must refer to a different target type.
   * 
   * @param functions  the functions
   * @return the calculation functions
   */
  public static CalculationFunctions of(List<? extends CalculationFunction<?>> functions) {
    return DefaultCalculationFunctions.of(functions.stream().collect(toImmutableMap(fn -> fn.targetType())));
  }

  /**
   * Obtains an instance from the specified functions.
   * <p>
   * This returns an implementation that matches the function by the type of the target.
   * When finding the matching function, the target type is looked up in the specified map.
   * The map will be validated to ensure the {@code Class} is consistent with
   * {@link CalculationFunction#targetType()}.
   * 
   * @param functions  the functions
   * @return the calculation functions
   */
  public static CalculationFunctions of(Map<Class<?>, ? extends CalculationFunction<?>> functions) {
    return DefaultCalculationFunctions.of(functions);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the function that handles the specified target.
   * <p>
   * If no function is found, a suitable default that can perform no calculations is provided.
   * 
   * @param <T>  the target type
   * @param target  the calculation target, such as a trade
   * @return the function
   */
  public default <T extends CalculationTarget> CalculationFunction<? super T> getFunction(T target) {
    return findFunction(target).orElse(MissingConfigCalculationFunction.INSTANCE);
  }

  /**
   * Finds the function that handles the specified target.
   * <p>
   * If no function is found the result is empty.
   * 
   * @param <T>  the target type
   * @param target  the calculation target, such as a trade
   * @return the function, empty if not found
   */
  public abstract <T extends CalculationTarget> Optional<CalculationFunction<? super T>> findFunction(T target);

  /**
   * Returns a set of calculation functions which combines the functions in this set with the functions in another.
   * <p>
   * If both sets of functions contain a function for a target then the function from this set is returned.
   *
   * @param other  another set of calculation functions
   * @return a set of calculation functions which combines the functions in this set with the functions in the other
   */
  public default CalculationFunctions composedWith(CalculationFunctions other) {
    return CompositeCalculationFunctions.of(this, other);
  }

  /**
   * Returns a set of calculation functions which combines the functions in this set with some
   * derived calculation functions.
   * <p>
   * Each derived function calculates one measure for one type of target, possibly using other calculated measures
   * as inputs.
   * <p>
   * If any of the derived functions depend on each other they must be passed to this method in the correct
   * order to ensure their dependencies can be satisfied. For example, if there is a derived function
   * {@code fnA} which depends on the measure calculated by function {@code fnB} they must be passed to
   * this method in the order {@code fnB, fnA}.
   *
   * @param functions  the functions
   * @return a set of calculation functions which combines the functions in this set with some
   * derived calculation functions
   */
  public default CalculationFunctions composedWith(DerivedCalculationFunction<?, ?>... functions) {
    return new DerivedCalculationFunctions(this, functions);
  }
}
