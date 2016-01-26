/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * An operation consuming two arguments - one {@code int} and one object.
 * <p>
 * Implementations of this interface will operate using side-effects.
 *
 * @param <T> the type of the object parameter
 */
@FunctionalInterface
public interface IntObjConsumer<T> {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue  the first argument
   * @param objectValue  the second argument
   */
  void accept(int intValue, T objectValue);

}
