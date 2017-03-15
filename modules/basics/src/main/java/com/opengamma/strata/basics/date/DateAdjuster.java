/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

/**
 * Functional interface that can adjust a date.
 * <p>
 * This extends {@link TemporalAdjuster} for those cases where the temporal to
 * be adjusted is an ISO-8601 date.
 */
@FunctionalInterface
public interface DateAdjuster
    extends TemporalAdjuster {

  /**
   * Adjusts the date according to the rules of the implementation.
   * <p>
   * Implementations must specify how the date is adjusted.
   * 
   * @param date  the date to adjust
   * @return the adjusted date
   * @throws DateTimeException if unable to make the adjustment
   * @throws ArithmeticException if numeric overflow occurs
   */
  public abstract LocalDate adjust(LocalDate date);

  /**
   * Adjusts the temporal according to the rules of the implementation.
   * <p>
   * This method implements {@link TemporalAdjuster} by calling {@link #adjust(LocalDate)}.
   * Note that conversion to {@code LocalDate} ignores the calendar system
   * of the input, which is the desired behaviour in this case.
   * 
   * @param temporal  the temporal to adjust
   * @return the adjusted temporal
   * @throws DateTimeException if unable to make the adjustment
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public default Temporal adjustInto(Temporal temporal) {
    // conversion to LocalDate ensures that other calendar systems are ignored
    return temporal.with(adjust(LocalDate.from(temporal)));
  }

}
