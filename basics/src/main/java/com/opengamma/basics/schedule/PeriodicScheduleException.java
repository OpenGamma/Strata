/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import com.opengamma.collect.ArgChecker;

/**
 * Exception thrown when a schedule cannot be calculated.
 */
public final class PeriodicScheduleException
    extends IllegalArgumentException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The invalid schedule definition.
   */
  private final PeriodicScheduleDefn definition;

  /**
   * Creates an instance.
   * <p>
   * The message is formatted using {@link ArgChecker#formatMessage(String, Object...)}.
   * 
   * @param msgTemplate  the message template
   * @param msgArguments  the message arguments
   * @param definition  the invalid schedule definition, may be null
   */
  public PeriodicScheduleException(PeriodicScheduleDefn definition, String msgTemplate, Object... msgArguments) {
    super(ArgChecker.formatMessage(msgTemplate, msgArguments));
    this.definition = definition;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the invalid schedule definition.
   * 
   * @return the definition, may be null
   */
  public PeriodicScheduleDefn getDefinition() {
    return definition;
  }

}
