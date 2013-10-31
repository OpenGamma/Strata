package com.opengamma.sesame;

/**
 * Represents the status of a function call, such that clients can use
 * it to determine whether they can continue, or need to pass failure
 * to their callers.
 */
public enum ResultStatus {
  /**
   * The function call completed successfully.
   */
  SUCCESS(true),
  /**
   * Indicates that a result has been returned but that some (or all)
   * market data is missing. Primarily for use for market data function calls.
   */
  MISSING_MARKET_DATA(true),
  /**
   * Indicates that a result has been returned but that some (or all)
   * market data is pending. Primarily for use for market data function calls.
   */
  AWAITING_MARKET_DATA(true),
  /**
   * Some aspect of the calculation in the function has failed and therefore
   * could not be completed.
   */
  CALCULATION_FAILED(false),
  /**
   * Some data required for the function was missing and therefore it could not
   * be successfully completed.
   */
  MISSING_DATA(false),
  /**
   * An exception was thrown during a function and therefore it could not
   * be successfully completed.
   */
  ERROR(false);

  private final boolean _isResultAvailable;

  private ResultStatus(boolean resultAvailable) {
    _isResultAvailable = resultAvailable;
  }

  /**
   * Indicates if a FunctionResult with this status has a return value populated.
   *
   * @return true if the FunctionResult has its return value populated
   */
  public boolean isResultAvailable() {
    return _isResultAvailable;
  }
}
