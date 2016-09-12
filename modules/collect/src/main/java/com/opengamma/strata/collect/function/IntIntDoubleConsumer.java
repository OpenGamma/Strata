/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * An operation consuming three arguments - {@code int}, {@code int} and {@code double}.
 * <p>
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
public interface IntIntDoubleConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @param doubleValue  the third argument
   */
  void accept(int intValue1, int intValue2, double doubleValue);

}
