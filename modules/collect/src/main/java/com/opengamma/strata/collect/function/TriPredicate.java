/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import java.util.Objects;

/**
 * A predicate that takes three arguments.
 *
 * @param <T> the type of the first object parameter
 * @param <U> the type of the second object parameter
 * @param <V> the type of the third object parameter
 */
@FunctionalInterface
public interface TriPredicate<T, U, V> {

  /**
   * Applies this predicate to the given arguments.
   *
   * @param t  the first predicate argument
   * @param u  the second predicate argument
   * @param v  the third predicate argument
   * @return whether the predicate matches
   */
  public abstract boolean test(T t, U u, V v);

  /**
   * Returns a new predicate that returns true if both predicates return true.
   * <p>
   * The second predicate is only invoked if the first returns true.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this AND that"
   */
  public default TriPredicate<T, U, V> and(TriPredicate<? super T, ? super U, ? super V> other) {
    Objects.requireNonNull(other);
    return (t, u, v) -> test(t, u, v) && other.test(t, u, v);
  }

  /**
   * Returns a new predicate that returns true if either predicates returns true.
   * <p>
   * The second predicate is only invoked if the first returns false.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this OR that"
   */
  public default TriPredicate<T, U, V> or(TriPredicate<? super T, ? super U, ? super V> other) {
    Objects.requireNonNull(other);
    return (t, u, v) -> test(t, u, v) || other.test(t, u, v);
  }

  /**
   * Returns a new predicate that negates the result of this predicate.
   *
   * @return the predicate, "NOT this"
   */
  public default TriPredicate<T, U, V> negate() {
    return (t, u, v) -> !test(t, u, v);
  }

}
