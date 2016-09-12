/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * An operation consuming two arguments - {@code int} and {@code double}.
 * <p>
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
public interface IntDoubleConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue  the first argument
   * @param doubleValue  the second argument
   */
  void accept(int intValue, double doubleValue);

}
