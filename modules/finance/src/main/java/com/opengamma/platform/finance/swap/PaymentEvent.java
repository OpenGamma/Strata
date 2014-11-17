/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;

import com.opengamma.basics.currency.CurrencyAmount;

/**
 * A payment event, where a single payment is made between two counterparties.
 * <p>
 * Implementations of this interface represent a single payment event.
 * Causes include exchange of notional amounts and fees.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface PaymentEvent
    extends ImmutableBean {

  /**
   * Gets the date that the payment is made.
   * <p>
   * Each payment period has a single payment date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  public abstract LocalDate getPaymentDate();

  /**
   * Gets the amount of the payment, positive if receiving, negative if paying.
   * <p>
   * This is the monetary value of the payment.
   * 
   * @return the payment amount
   */
  public abstract CurrencyAmount getPaymentAmount();

}
