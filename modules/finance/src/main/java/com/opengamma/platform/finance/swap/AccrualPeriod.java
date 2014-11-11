/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;

/**
 * A period over which interest is accrued.
 * <p>
 * A swap leg is designed to accrue interest over one or more periods.
 * Each basic period that generates interest is an accrual period.
 * For example, a fixed rate accrual period generates interest based on a fixed rate
 * for the duration of the period.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface AccrualPeriod
    extends ImmutableBean {

  /**
   * Gets the start date of the period.
   * <p>
   * This is the first accrual date in the period.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  public LocalDate getStartDate();

  /**
   * Gets the end date of the period.
   * <p>
   * This is the last accrual date in the period.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the period
   */
  public LocalDate getEndDate();

}
