/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import java.util.Optional;

import com.opengamma.collect.ArgChecker;

/**
 * Exception thrown when a schedule cannot be calculated.
 */
public final class ScheduleException
    extends IllegalArgumentException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The invalid schedule definition.
   */
  private final PeriodicSchedule definition;

  /**
   * Creates an instance.
   * <p>
   * The message is formatted using {@link ArgChecker#formatMessage(String, Object...)}.
   * 
   * @param msgTemplate  the message template
   * @param msgArguments  the message arguments
   */
  public ScheduleException(String msgTemplate, Object... msgArguments) {
    super(ArgChecker.formatMessage(msgTemplate, msgArguments));
    this.definition = null;
  }

  /**
   * Creates an instance, specifying the definition that caused the problem.
   * <p>
   * The message is formatted using {@link ArgChecker#formatMessage(String, Object...)}.
   * 
   * @param definition  the invalid schedule definition
   * @param msgTemplate  the message template
   * @param msgArguments  the message arguments
   */
  public ScheduleException(PeriodicSchedule definition, String msgTemplate, Object... msgArguments) {
    super(ArgChecker.formatMessage(msgTemplate, msgArguments));
    this.definition = definition;  // not validating for non-null to avoid exceptions from exceptions
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the invalid schedule definition.
   * 
   * @return the optional definition
   */
  public Optional<PeriodicSchedule> getDefinition() {
    return Optional.ofNullable(definition);
  }

}
