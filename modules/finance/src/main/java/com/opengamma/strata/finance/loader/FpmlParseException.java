/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.loader;

/**
 * Exception thrown when parsing FpML.
 */
public final class FpmlParseException
    extends RuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance based on a message.
   * 
   * @param message  the message, null tolerant
   */
  public FpmlParseException(String message) {
    super(message);
  }

  /**
   * Creates an instance based on a cause.
   * 
   * @param cause  the cause, null tolerant
   */
  public FpmlParseException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates an instance based on a message and cause.
   * 
   * @param message  the message, null tolerant
   * @param cause  the cause, null tolerant
   */
  public FpmlParseException(String message, Throwable cause) {
    super(message, cause);
  }

}
