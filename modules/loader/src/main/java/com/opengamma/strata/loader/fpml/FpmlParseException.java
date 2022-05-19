/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.ParseFailureException;

/**
 * Exception thrown when parsing FpML.
 */
public final class FpmlParseException extends ParseFailureException {

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
   * Creates an instance based on a message template.
   * <p>
   * This uses placeholders such as '{lineNumber}' to capture important information about the parse failure.
   * See {@link Messages#formatWithAttributes(String, Object...)}.
   * 
   * @param messageTemplate  the message template, null tolerant
   * @param messageArgs  the message arguments, null tolerant
   */
  public FpmlParseException(String messageTemplate, Object... messageArgs) {
    super(messageTemplate, messageArgs);
  }

}
