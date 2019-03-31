/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * A function of two arguments - one object and one {@code double} - that returns a {@code double}.
 * <p>
 * This takes two arguments and returns an object result.
 *
 * @param <T> the type of the object parameter
 */
@FunctionalInterface
public interface ObjDoubleToDoubleFunction<T> {

  /**
   * Applies the function.
   *
   * @param obj  the first argument
   * @param value  the second argument
   * @return the result of the function
   */
  public abstract double apply(T obj, double value);

  /**
   * Returns a new function that composes this function and the specified function.
   * <p>
   * This returns a composed function that applies the input to this function
   * and then converts the result using the specified function.
   *
   * @param other  the second function
   * @return the combined function, "this AND_THEN that"
   * @throws NullPointerException if the other function is null
   */
  public default ObjDoubleToDoubleFunction<T> andThen(DoubleUnaryOperator other) {
    Objects.requireNonNull(other);
    return (obj, value) -> other.applyAsDouble(apply(obj, value));
  }

}
