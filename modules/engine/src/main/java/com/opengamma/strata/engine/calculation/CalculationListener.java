/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculation;

/**
 * Listener that is notified when calculations are performed by a {@link CalculationRunner}.
 * <p>
 * It is guaranteed that the methods of a listener will only be invoked by a single thread at any
 * time. Therefore listener implementations are not necessarily required to be thread safe.
 * <p>
 * It is not guaranteed to be the same thread invoking a listener each time.
 */
public interface CalculationListener {

  /**
   * Invoked when a calculation completes.
   *
   * @param result  the result of the calculation
   */
  public abstract void resultReceived(CalculationResult result);

  /**
   * Invoked when all calculations have completed.
   * <p>
   * This is guaranteed to be called after all results have been passed to {@link #resultReceived}.
   */
  public abstract void calculationsComplete();
}
