/**
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
   * When a function is called it may produce zero to many errors.
   * If there is one error then that reason is used.
   * If there are many errors then the overall reason is "multiple".
   */
  MULTIPLE,
  /**
   * There was an exception thrown during the function call.
   * Where possible, more specific reason codes should be used.
   */
  ERROR,
  /**
   * There was no applicable calculation to be performed.
   * This is used to indicate a result that was not or could not be calculated,
   * but where calculation was not desired or applicable. This might occur in a
   * grid of results where not every column is applicable for a specific row.
   */
  NOT_APPLICABLE,
  /**
   * The input was invalid.
   * This is used if no configured function matched the specified input object.
   */
  INVALID_INPUT,
  /**
   * No value was provided for a non-nullable argument.
   * When running a function, values must be available for all parameters.
   * This reason indicates that a value was missing.
   */
  MISSING_ARGUMENT,
  /**
   * Some data required for the function was missing.
   * When running a function, all the necessary data must be available.
   * This reason indicates that a piece of data was missing.
   */
  MISSING_DATA,
  /**
   * Some aspect of the calculation in the function has failed.
   */
  CALCULATION_FAILED,
  /**
   * Some data required for the function has been requested but not received and
   * therefore the function could not be successfully completed.
   * If the function is retried the calculation may succeed.
   */
  PENDING_DATA,
  /**
   * The user has insufficient permissions to view the result.
   */
  PERMISSION_DENIED,
  /**
   * Failure occurred for some other reason.
   * This reason should only be used when no other type is applicable.
   * If using this reason, please consider raising an issue to get another
   * more descriptive reason added.
   */
  OTHER;

}
