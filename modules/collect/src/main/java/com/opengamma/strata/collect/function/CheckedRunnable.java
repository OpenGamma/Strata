/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code Runnable}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 */
@FunctionalInterface
public interface CheckedRunnable {

  /**
   * Performs an action.
   *
   * @throws Throwable if an error occurs
   */
  public void run() throws Throwable;

}
