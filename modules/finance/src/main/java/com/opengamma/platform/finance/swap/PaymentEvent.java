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
   * Each payment event has a single payment date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the payment date
   */
  public abstract LocalDate getPaymentDate();

  /**
   * Gets the currency of the event.
   * <p>
   * An event has a single currency.
   * 
   * @return the currency of the event
   */
  public abstract Currency getCurrency();

}
