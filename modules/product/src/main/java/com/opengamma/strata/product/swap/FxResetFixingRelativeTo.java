/**
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
 * The base date that each FX reset fixing is made relative to.
 * <p>
 * When calculating the FX reset fixing dates for a swap leg, the date is calculated relative to another date.
 * The other date is specified by this enum.
 */
public enum FxResetFixingRelativeTo {

  /**
   * The FX reset fixing is made relative to the start of the first accrual period.
   * <p>
   * The fixing date is relative to the start date of the first accrual period
   * within the payment period, as adjusted by business day conventions.
   */
  PERIOD_START,
  /**
   * The FX reset fixing is made relative to the end of the last accrual period.
   * <p>
   * The fixing date is relative to the end date of the last accrual period
   * within the payment period, as adjusted by business day conventions.
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
  public static FxResetFixingRelativeTo of(String uniqueName) {
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
