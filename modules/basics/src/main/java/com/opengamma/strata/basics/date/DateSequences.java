/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard date sequences.
 * <p>
 * This class provides instances of {@link DateSequence} representing standard financial industry
 * sequences of dates. The most common are the quarterly IMM dates, which are on the third
 * Wednesday of March, June, September and December.
 * <p>
 * Additional date sequences may be registered by name using {@code DateSequence.ini}.
 */
public final class DateSequences {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<DateSequence> ENUM_LOOKUP = ExtendedEnum.of(DateSequence.class);

  /**
   * The 'Quarterly-IMM' date sequence.
   * <p>
   * An instance defining the sequence of quarterly IMM dates.
   * The quarterly IMM dates are the third Wednesday of March, June, September and December.
   */
  public static final DateSequence QUARTERLY_IMM = DateSequence.of(StandardDateSequences.QUARTERLY_IMM.getName());
  /**
   * The 'Monthly-IMM' date sequence.
   * <p>
   * An instance defining the sequence of monthly IMM dates.
   * The monthly IMM dates are the third Wednesday of each month.
   */
  public static final DateSequence MONTHLY_IMM = DateSequence.of(StandardDateSequences.MONTHLY_IMM.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private DateSequences() {
  }

}
