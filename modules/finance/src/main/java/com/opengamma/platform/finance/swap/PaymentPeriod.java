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
 * A period over which interest is accrued with a single payment.
 * <p>
 * A single payment period within a swap leg.
 * The amount of the payment is defined by implementations of this interface.
 * It is typically based on a rate of interest.
 * <p>
 * This interface imposes few restrictions on the payment periods.
 * The period must have a payment date, currency and period dates.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface PaymentPeriod
    extends ImmutableBean {

  /**
   * Gets the date that the payment is made.
   * <p>
   * Each payment period has a single payment date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the payment date of the period
   */
  public abstract LocalDate getPaymentDate();

  /**
   * Gets the currency of the payment resulting from the period.
   * <p>
   * This is the currency of the generated payment.
   * A period has a single currency.
   * 
   * @return the currency of the period
   */
  public abstract Currency getCurrency();

  /**
   * Gets the start date of the period.
   * <p>
   * This is the first accrual date in the period.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  public abstract LocalDate getStartDate();

  /**
   * Gets the end date of the period.
   * <p>
   * This is the last accrual date in the period.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the period
   */
  public abstract LocalDate getEndDate();

}
