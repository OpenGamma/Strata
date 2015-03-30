/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * A predicate of two arguments - one object and one {@code long}.
 * <p>
 * This takes two arguments and returns a {@code boolean} result.
 *
 * @param <T> the type of the object parameter
 * @see BiPredicate
 */
@FunctionalInterface
public interface ObjLongPredicate<T> {

  /**
   * Evaluates the predicate.
   *
   * @param obj  the first argument
   * @param value  the second argument
   * @return true if the arguments match the predicate
   */
  boolean test(T obj, long value);

  /**
   * Returns a new predicate that returns true if both predicates return true.
   * <p>
   * The second predicate is only invoked if the first returns true.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this AND that"
   * @throws NullPointerException if the other predicate is null
   */
  default ObjLongPredicate<T> and(ObjLongPredicate<? super T> other) {
    Objects.requireNonNull(other);
    return (obj, value) -> test(obj, value) && other.test(obj, value);
  }

  /**
   * Returns a new predicate that returns true if either predicates returns true.
   * <p>
   * The second predicate is only invoked if the first returns false.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this OR that"
   * @throws NullPointerException if the other predicate is null
   */
  default ObjLongPredicate<T> or(ObjLongPredicate<? super T> other) {
    Objects.requireNonNull(other);
    return (obj, value) -> test(obj, value) || other.test(obj, value);
  }

  /**
   * Returns a new predicate that negates the result of this predicate.
   *
   * @return the predicate, "NOT this"
   */
  default ObjLongPredicate<T> negate() {
    return (obj, value) -> !test(obj, value);
  }

}
