/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * A function that takes three arguments.
 *
 * @param <T> the type of the first object parameter
 * @param <U> the type of the second object parameter
 * @param <V> the type of the third object parameter
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {

  /**
   * Applies this function to the given arguments.
   *
   * @param t  the first function argument
   * @param u  the second function argument
   * @param v  the third function argument
   * @return the function result
   */
  public abstract R apply(T t, U u, V v);

  /**
   * Returns a new function that composes this function and the specified function.
   * <p>
   * This returns a composed function that applies the input to this function
   * and then converts the result using the specified function.
   *
   * @param <S> the type of the resulting function
   * @param after  the function to combine with
   * @return the combined function, "this AND_THEN that"
   */
  public default <S> TriFunction<T, U, V, S> andThen(Function<? super R, ? extends S> after) {
    Objects.requireNonNull(after);
    return (T t, U u, V v) -> after.apply(apply(t, u, v));
  }

}
