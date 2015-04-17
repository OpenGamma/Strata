/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

/**
 * Exception thrown when pricing fails.
 */
public final class PricingException
    extends RuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance based on a message.
   * 
   * @param message  the message, null tolerant
   */
  public PricingException(String message) {
    super(message);
  }

  /**
   * Creates an instance based on a message and cause.
   * 
   * @param message  the message, null tolerant
   * @param cause  the cause, null tolerant
   */
  public PricingException(String message, Throwable cause) {
    super(message, cause);
  }

}
