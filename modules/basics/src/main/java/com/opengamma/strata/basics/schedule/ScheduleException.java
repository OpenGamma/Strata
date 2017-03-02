/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import java.util.Optional;

import com.opengamma.strata.collect.Messages;

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
   * The message is formatted using {@link Messages#format(String, Object...)}.
   * Message formatting is null tolerant to avoid hiding this exception.
   * 
   * @param msgTemplate  the message template, null tolerant
   * @param msgArguments  the message arguments, null tolerant
   */
  public ScheduleException(String msgTemplate, Object... msgArguments) {
    this(null, msgTemplate, msgArguments);
  }

  /**
   * Creates an instance, specifying the definition that caused the problem.
   * <p>
   * The message is formatted using {@link Messages#format(String, Object...)}.
   * Message formatting is null tolerant to avoid hiding this exception.
   * 
   * @param definition  the invalid schedule definition, null tolerant
   * @param msgTemplate  the message template, null tolerant
   * @param msgArguments  the message arguments, null tolerant
   */
  public ScheduleException(PeriodicSchedule definition, String msgTemplate, Object... msgArguments) {
    super(Messages.format(msgTemplate, msgArguments));
    this.definition = definition;
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
