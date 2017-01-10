/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.List;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;

/**
 * Listener that is notified when calculations are performed by a {@link CalculationRunner}.
 * <p>
 * It is guaranteed that the methods of a listener will only be invoked by a single thread at any
 * time. It is not guaranteed to be the same thread invoking a listener each time. The calling
 * code is synchronized to ensure that any changes in the listener state will be
 * visible to every thread used to invoke the listener. Therefore listener implementations
 * are not required to be thread safe.
 * <p>
 * A listener instance should not be used for multiple sets of calculations.
 */
public interface CalculationListener {

  /**
   * Invoked when the calculations start; guaranteed to be invoked
   * before {@link #resultReceived(CalculationTarget, CalculationResult)} and
   * {@link #calculationsComplete()}.
   *
   * @param targets the targets for which values are being calculated; these are often trades
   * @param columns the columns for which values are being calculated
   */
  public default void calculationsStarted(List<CalculationTarget> targets, List<Column> columns) {
    // Default implementation does nothing, required for backwards compatibility
  }

  /**
   * Invoked when a calculation completes.
   * <p>
   * It is guaranteed that {@link #calculationsStarted(List, List)} will be called before
   * this method and that this method will never be called after {@link #calculationsComplete()}.
   * <p>
   * It is possible that this method will never be called. This can happen if an empty list of targets
   * is passed to the calculation runner.
   *
   * @param target  the calculation target, such as a trade
   * @param result  the result of the calculation
   */
  public abstract void resultReceived(CalculationTarget target, CalculationResult result);

  /**
   * Invoked when all calculations have completed.
   * <p>
   * This is guaranteed to be called after all results have been passed to {@link #resultReceived}.
   * <p>
   * This method will be called immediately after {@link #calculationsStarted(List, List)} and without any calls
   * to {@link #resultReceived(CalculationTarget, CalculationResult)} if there are no calculations to be performed.
   * This can happen if an empty list of targets is passed to the calculation runner.
   */
  public abstract void calculationsComplete();

}
