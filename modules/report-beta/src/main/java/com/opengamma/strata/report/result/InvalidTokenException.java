/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import com.opengamma.strata.collect.Messages;

/**
 * Thrown to indicate that a token was not valid for a given target.
 */
public class InvalidTokenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception.
   * 
   * @param token  the invalid token
   * @param targetType  the target type against which it was evaluated
   */
  public InvalidTokenException(String token, Class<?> targetType) {
    super(Messages.format("Invalid field '{}' on {}", token, targetType.getSimpleName()));
  }

}
