/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

/**
 * Exception thrown if reference data cannot be found.
 */
public class ReferenceDataNotFoundException extends RuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception passing the exception message.
   *
   * @param message  the exception message, null tolerant
   */
  public ReferenceDataNotFoundException(String message) {
    super(message);
  }

}
