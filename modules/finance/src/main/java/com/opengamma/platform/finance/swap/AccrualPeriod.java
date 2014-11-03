/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;

import com.opengamma.basics.currency.Currency;

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
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * 
   * @return the start date of the period
   */
  public LocalDate getStartDate();

  /**
   * Gets the end date of the period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * 
   * @return the end date of the period
   */
  public LocalDate getEndDate();

  /**
   * Gets the currency of the period.
   * <p>
   * A period has a single currency.
   * 
   * @return the currency of the period
   */
  public Currency getCurrency();

}
