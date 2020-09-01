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
   * The 'Quarterly-IMM-6-Serial' date sequence.
   * <p>
   * An instance defining the sequence of 6 serial monthly IMM dates followed by the standard quarterly IMM dates.
   * The IMM dates are the third Wednesday of each month.
   * The quarterly months are March, June, September and December.
   * The base sequence consists of just the quarterly IMM dates.
   * The full sequence consists of the six serial months before joining the base quarterly sequence.
   */
  public static final DateSequence QUARTERLY_IMM_6_SERIAL =
      DateSequence.of(StandardDateSequences.QUARTERLY_IMM_6_SERIAL.getName());
  /**
   * The 'Quarterly-IMM-3-Serial' date sequence.
   * <p>
   * An instance defining the sequence of 3 serial monthly IMM dates followed by the standard quarterly IMM dates.
   * The IMM dates are the third Wednesday of each month.
   * The quarterly months are March, June, September and December.
   * The base sequence consists of just the quarterly IMM dates.
   * The full sequence consists of the three serial months before joining the base quarterly sequence.
   */
  public static final DateSequence QUARTERLY_IMM_3_SERIAL =
      DateSequence.of(StandardDateSequences.QUARTERLY_IMM_3_SERIAL.getName());
  /**
   * The 'Monthly-IMM' date sequence.
   * <p>
   * An instance defining the sequence of monthly IMM dates.
   * The monthly IMM dates are the third Wednesday of each month.
   */
  public static final DateSequence MONTHLY_IMM = DateSequence.of(StandardDateSequences.MONTHLY_IMM.getName());
  /**
   * The 'Quarterly-10th' date sequence.
   * <p>
   * An instance defining the sequence of quarterly dates on the 10th of each month.
   * The quarterly months are March, June, September and December.
   */
  public static final DateSequence QUARTERLY_10TH = DateSequence.of(StandardDateSequences.QUARTERLY_10TH.getName());
  /**
   * The 'Monthly-1st' date sequence, equivalent to a sequence of calendar months.
   * <p>
   * An instance defining the sequence of monthly dates on the 1st of each month.
   */
  public static final DateSequence MONTHLY_1ST = DateSequence.of(StandardDateSequences.MONTHLY_1ST.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private DateSequences() {
  }

}
