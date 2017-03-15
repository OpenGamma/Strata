/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * An operation consuming two arguments - {@code int} and {@code int}.
 * <p>
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
public interface IntIntConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   */
  void accept(int intValue1, int intValue2);

}
