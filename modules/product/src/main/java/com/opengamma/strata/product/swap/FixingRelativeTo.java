/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The base date that each rate fixing is made relative to.
 * <p>
 * When calculating the rate fixing dates for a swap leg, the date is calculated relative to another date.
 * The other date is specified by this enum.
 */
public enum FixingRelativeTo {

  /**
   * The rate fixing is made relative to the start of each reset period.
   * <p>
   * The fixing date is relative to the start date of each reset period
   * within the accrual period, as adjusted by business day conventions.
   * <p>
   * This can be referred to as "fixing in advance" or "reset in advance".
   */
  PERIOD_START,
  /**
   * The rate fixing is made relative to the end of each reset period.
   * <p>
   * The fixing date is relative to the end date of each reset period
   * within the accrual period, as adjusted by business day conventions.
   * <p>
   * This can be referred to as "fixing in arrears" or "reset in arrears".
   */
  PERIOD_END;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FixingRelativeTo of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
  // selects the base date for fixing
  LocalDate selectBaseDate(SchedulePeriod period) {
    return (this == PERIOD_END ? period.getEndDate() : period.getStartDate());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
