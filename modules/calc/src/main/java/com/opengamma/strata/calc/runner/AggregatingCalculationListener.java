/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.opengamma.strata.basics.CalculationTarget;

/**
 * Superclass for mutable calculation listeners that collect the results of individual calculations and
 * create a single aggregate result when the calculations are complete.
 * 
 * @param <T>  the type of the aggregate result
 */
public abstract class AggregatingCalculationListener<T>
    implements CalculationListener {

  /** A future representing the aggregate result. */
  private final CompletableFuture<T> future = new CompletableFuture<>();

  @Override
  public final void calculationsComplete() {
    future.complete(createAggregateResult());
  }

  /**
   * Returns the aggregate result of the calculations, blocking until it is available.
   * <p>
   * If the thread is interrupted while this method is blocked, then a runtime exception
   * is thrown, but with the interrupt flag set.
   * For additional control, use {@link #getFuture()}.
   *
   * @return the aggregate result of the calculations, blocking until it is available
   */
  public T result() {
    try {
      return future.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    } catch (ExecutionException ex) {
      throw new RuntimeException("Exception getting result", ex);
    }
  }

  /**
   * A future providing asynchronous notification when the results are available.
   *
   * @return a future providing asynchronous notification when the results are available
   */
  public CompletableFuture<T> getFuture() {
    return future;
  }

  @Override
  public abstract void resultReceived(CalculationTarget target, CalculationResult result);

  /**
   * Invoked to create the aggregate result when the individual calculations are complete.
   * <p>
   * This is guaranteed to be invoked after all results have been passed to {@link #resultReceived}.
   *
   * @return the aggregate result of all the calculations
   */
  protected abstract T createAggregateResult();
}
