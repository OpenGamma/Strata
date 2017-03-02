/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * A payment event, where a single payment is made between two counterparties.
 * <p>
 * Implementations of this interface represent a single payment event.
 * Causes include exchange of notional amounts and fees.
 * <p>
 * This interface imposes few restrictions on the payment events.
 * The event must have a payment date and currency.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface SwapPaymentEvent {

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
   * Gets the currency of the payment resulting from the event.
   * <p>
   * This is the currency of the generated payment.
   * An event has a single currency.
   * 
   * @return the currency of the payment
   */
  public abstract Currency getCurrency();

  //-------------------------------------------------------------------------
  /**
   * Adjusts the payment date using the rules of the specified adjuster.
   * <p>
   * The adjuster is typically an instance of {@link BusinessDayAdjustment}.
   * Implementations must return a new instance unless they are immutable and no change occurs.
   * 
   * @param adjuster  the adjuster to apply to the payment date
   * @return the adjusted payment event
   */
  SwapPaymentEvent adjustPaymentDate(TemporalAdjuster adjuster);

}
