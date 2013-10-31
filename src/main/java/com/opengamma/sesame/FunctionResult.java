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
 * @param <T> the type of the underlying result, for a successful invocation
 */
public interface FunctionResult<T> {

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

  <N> FunctionResult<N> generateSuccessResult(SuccessStatus status, N newResult);

  <N> FunctionResult<N> generateFailureResult(FailureStatus status, String message, Object... args);

  /**
   * Generate a new failure result with the same details as this one but potentially
   * with a new result type.
   *
   * @param <N> the result type
   * @return a new failure result
   */
  <N> FunctionResult<N> generateFailureResult();

  String getFailureMessage();
}
