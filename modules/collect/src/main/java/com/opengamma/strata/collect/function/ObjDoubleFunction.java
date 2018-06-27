/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A function of two arguments - one object and one {@code double}.
 * <p>
 * This takes two arguments and returns an object result.
 *
 * @param <T> the type of the object parameter
 * @param <R> the type of the result
 * @see BiFunction
 */
@FunctionalInterface
public interface ObjDoubleFunction<T, R> {

  /**
   * Applies the function.
   *
   * @param obj  the first argument
   * @param value  the second argument
   * @return the result of the function
   */
  public abstract R apply(T obj, double value);

  /**
   * Returns a new function that composes this function and the specified function.
   * <p>
   * This returns a composed function that applies the input to this function
   * and then converts the result using the specified function.
   *
   * @param <V> the result type of second function
   * @param other  the second function
   * @return the combined function, "this AND_THEN that"
   * @throws NullPointerException if the other function is null
   */
  public default <V> ObjDoubleFunction<T, V> andThen(Function<? super R, ? extends V> other) {
    Objects.requireNonNull(other);
    return (obj, value) -> other.apply(apply(obj, value));
  }

}
