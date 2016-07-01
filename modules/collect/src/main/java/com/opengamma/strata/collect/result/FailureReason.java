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
   * An error occurred during the calculation.
   * Where possible, more specific reason codes should be used.
   */
  ERROR,
  /**
   * The request was invalid.
   * This is used to indicate that the request was not valid.
   */
  INVALID,
  /**
   * There was no applicable calculation to be performed.
   * This is used to indicate a result that was not or could not be calculated,
   * but where calculation was not desired or applicable. This might occur in a
   * grid of results where not every column is applicable for a specific row.
   */
  NOT_APPLICABLE,
  /**
   * The request is not supported.
   * This is used to indicate that the request is not supported and could not be calculated.
   */
  UNSUPPORTED,
  /**
   * Some data was missing.
   * When performing calculations, all the necessary data must be available.
   * This reason indicates that a piece of data was missing.
   */
  MISSING_DATA,
  /**
   * Currency conversion failed.
   * An error occurred during currency conversion, perhaps due to missing FX rates.
   */
  CURRENCY_CONVERSION,
  /**
   * Some aspect of the calculation in the function has failed.
   */
  CALCULATION_FAILED,
  /**
   * Failure occurred for some other reason.
   * This reason should only be used when no other type is applicable.
   * If using this reason, please consider raising an issue to get another
   * more descriptive reason added.
   */
  OTHER;

}
