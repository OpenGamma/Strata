/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import java.util.Objects;

/**
 * A consumer that takes three arguments.
 *
 * @param <T> the type of the first object parameter
 * @param <U> the type of the second object parameter
 * @param <V> the type of the third object parameter
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

  /**
   * Applies this consumer to the given arguments.
   *
   * @param t  the first consumer argument
   * @param u  the second consumer argument
   * @param v  the third consumer argument
   */
  public abstract void accept(T t, U u, V v);

  /**
   * Returns a new consumer that composes this consumer and the specified consumer.
   * <p>
   * This returns a composed consumer that first calls this consumer and then calls
   * the specified consumer.
   *
   * @param after  the consumer to combine with
   * @return the combined consumer, "this AND_THEN that"
   */
  public default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
    Objects.requireNonNull(after);
    return (T t, U u, V v) -> {
      accept(t, u, v);
      after.accept(t, u, v);
    };
  }

}
