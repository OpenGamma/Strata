/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

public enum FailureStatus implements ResultStatus {
  /**
   * Some data required for the function was missing and therefore it could not
   * be successfully completed.
   */
  MISSING_DATA,
  /**
   * An exception was thrown during a function and therefore it could not
   * be successfully completed.
   */
  ERROR,
  /**
   * Some aspect of the calculation in the function has failed and therefore
   * could not be completed.
   */
  CALCULATION_FAILED;

  @Override
  public boolean isResultAvailable() {
    return false;
  }
}
