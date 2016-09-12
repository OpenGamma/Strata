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
 * The base date that each payment is made relative to.
 * <p>
 * When calculating the payment date for a swap leg, the date is calculated relative to another date.
 * The other date is specified by this enum.
 */
public enum PaymentRelativeTo {

  /**
   * The payment is made relative to the start of each payment period.
   * <p>
   * The payment date is relative to the start date of the first accrual period
   * within the payment period, as adjusted by business day conventions.
   * <p>
   * This can be referred to as "payment in advance".
   */
  PERIOD_START,
  /**
   * The payment is made relative to the end of each payment period.
   * <p>
   * The payment date is relative to the end date of the last accrual period
   * within the payment period, as adjusted by business day conventions.
   * <p>
   * This can be referred to as "payment in arrears".
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
  public static PaymentRelativeTo of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
  // selects the base date for payment
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
