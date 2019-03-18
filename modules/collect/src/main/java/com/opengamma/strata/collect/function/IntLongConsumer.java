/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * An operation consuming two arguments - {@code int} and {@code long}.
 * <p>
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
public interface IntLongConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue  the first argument
   * @param longValue  the second argument
   */
  public abstract void accept(int intValue, long longValue);

}
