/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * Represents the status of a function call, such that clients can use
 * it to determine whether they can continue, or need to pass failure
 * to their callers.
 */
public enum SuccessStatus implements ResultStatus {
  /**
   * The function call completed successfully.
   */
  SUCCESS,
  /**
   * Indicates that a result has been returned but that some (or all)
   * market data is missing. Primarily for use for market data function calls.
   */
  MISSING_MARKET_DATA,
  /**
   * Indicates that a result has been returned but that some (or all)
   * market data is pending. Primarily for use for market data function calls.
   */
  AWAITING_MARKET_DATA;

  public boolean isResultAvailable() {
    return true;
  }
}
