/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

// TODO rename failure reason? create FailureStatus marker interface it can implement?
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
  CALCULATION_FAILED,
  /**
   * There were multiple failures of different types during execution of the function.
   */
  MULTIPLE;

  @Override
  public boolean isResultAvailable() {
    return false;
  }
}
