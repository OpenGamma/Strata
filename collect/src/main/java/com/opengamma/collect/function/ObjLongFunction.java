/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.function;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A function of two arguments - one object and one {@code long}.
 * <p>
 * This takes two arguments and returns an object result.
 *
 * @param <T> the type of the object parameter
 * @param <R> the type of the result
 * @see BiFunction
 */
@FunctionalInterface
public interface ObjLongFunction<T, R> {

  /**
   * Evaluates the predicate.
   *
   * @param t  the first argument
   * @param u  the second argument
   * @return true if the arguments match the predicate
   */
  R apply(T t, long u);

  /**
   * Returns a new function that combines this function and the specified function.
   * <p>
   * The result of this function is passed into the specified function.
   *
   * @param <V> the result type of second function
   * @param other  the second function
   * @return the combined function, "this AND_THEN that"
   * @throws NullPointerException if the other function is null
   */
  default <V> ObjLongFunction<T, V> andThen(Function<? super R, ? extends V> other) {
    Objects.requireNonNull(other);
    return (T t, long u) -> other.apply(apply(t, u));
  }

}
