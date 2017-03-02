/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code Supplier}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface CheckedSupplier<R> {

  /**
   * Gets a result.
   *
   * @return a result
   * @throws Throwable if an error occurs
   */
  public R get() throws Throwable;

}
