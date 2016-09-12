/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math;

/**
 * Exception thrown by math.
 */
public class MathException extends RuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   */
  public MathException() {
    super();
  }

  /**
   * Creates an instance based on a message.
   * 
   * @param message  the message, null tolerant
   */
  public MathException(String message) {
    super(message);
  }

  /**
   * Creates an instance based on a message and cause.
   * 
   * @param message  the message, null tolerant
   * @param cause  the cause, null tolerant
   */
  public MathException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates an instance based on a cause.
   * 
   * @param cause  the cause, null tolerant
   */
  public MathException(Throwable cause) {
    super(cause);
  }

}
