/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * The immutable result from a function containing an indication
 * of whether a value has been calculated. FunctionResults from
 * multiple function calls can be combined using the combine method.
 *
 * T the type of the underlying result, for a successful invocation
 */
public interface FunctionResult<T> {

  /**
   * Combine this result with the passed results. This result has the
   * highest priority and the passed results will be processed in order
   * with an earlier result having priority over a later one.
   *
   * @param functionResults the results to be combined with this one
   * @return a new combined result
   */
  FunctionResult combine(FunctionResult... functionResults);

  /**
   * Get the set of market data that have been reported as required.
   *
   * @return
   */
  RequiredMarketData getRequiredMarketData();

  /**
   * Indicates the status of this result. It is up to the client to decide
   * if it is able to handle the status or decline to handle.
   *
   * @return the status of this function result
   */
  ResultStatus getStatus();

  /**
   * Return the actual result if calculated successfully. If it has not been
   * calculated then an IllegalStateException will be thrown. To avoid this
   * check the result status using {@link #getStatus()} first.
   *
   * @return the result if calculated successfully, not null
   * @throws IllegalArgumentException if called when the result has not been
   * successfully calculated
   */
  T getResult();

}
