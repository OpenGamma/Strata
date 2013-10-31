/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

public interface ResultStatus {

  /**
   * Indicates if a FunctionResult with this status has a return value populated.
   *
   * @return true if the FunctionResult has its return value populated
   */
  boolean isResultAvailable();

}
