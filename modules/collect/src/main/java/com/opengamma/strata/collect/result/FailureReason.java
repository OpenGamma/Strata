/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

/**
 * Represents the reason why failure occurred.
 * <p>
 * Each failure is categorized as one of the following reasons.
 */
public enum FailureReason {

  /**
   * There were multiple failures of different types.
   * <p>
   * An operation may produce zero to many errors.
   * If there is one error then that reason is used.
   * If there are many errors then the overall reason is "multiple".
   */
  MULTIPLE,
  /**
   * An error occurred.
   * <p>
   * Where possible, a more specific reason code should be used.
   */
  ERROR,
  /**
   * The input was invalid.
   * <p>
   * One or more input parameters was invalid.
   */
  INVALID,
  /**
   * A parsing error occurred.
   * <p>
   * This is used when an error occurred during parsing.
   * Typically, this refers to parsing a file, such as CSV or XML.
   */
  PARSING,
  /**
   * The operation requested was not applicable.
   * <p>
   * This is used when the particular combination of inputs is not applicable,
   * but given a different combination a result could have been calculated.
   * For example, this might occur in a grid of results where the calculation
   * requested for a column is not applicable for every row.
   */
  NOT_APPLICABLE,
  /**
   * The operation requested is unsupported.
   * <p>
   * The operation failed because it is not supported.
   */
  UNSUPPORTED,
  /**
   * The operation failed because data was missing.
   * <p>
   * One or more pieces of data that the operation required were missing.
   */
  MISSING_DATA,
  /**
   * Currency conversion failed.
   * <p>
   * This is used to indicate that the operation failed during currency conversion, perhaps due to missing FX rates.
   */
  CURRENCY_CONVERSION,
  /**
   * The operation could not be performed.
   * <p>
   * This is used to indicate that a calculation failed.
   */
  CALCULATION_FAILED,
  /**
   * Failure occurred for some other reason.
   * <p>
   * This reason should only be used when no other type is applicable.
   * If using this reason, please consider raising an issue to get another
   * more descriptive reason added.
   */
  OTHER;

}
