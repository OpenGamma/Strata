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
   * check the result status using {@link #isResultAvailable()} or
   * {@link #getStatus()} first.
   *
   * @return the result if calculated successfully, not null
   * @throws IllegalArgumentException if called when the result has not been
   * successfully calculated
   */
  T getResult();

  /**
   * Return the message associated with a failure event. If the calculation
   * was actually successful then an an IllegalStateException will be thrown.
   * To avoid this check the result status using {@link #isResultAvailable()}
   * or {@link #getStatus()} first.
   *
   * @return the failure message if calculation was unsuccessful, not null
   * @throws IllegalArgumentException if called when the result has been
   * successfully calculated
   */
  String getFailureMessage();

  /**
   * Indicates if there is a result available from this instance. This
   * generally means that any calculation has been successfully performed
   * but for calculation that may return partial results e.g. market data
   * requests this method will return true. To distinguish between these
   * cases, check the result status using {@link #getStatus()}.
   *
   * @return true if a result is available
   */
  boolean isResultAvailable();
}
