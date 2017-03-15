/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.index.Index;

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
public interface SwapPaymentPeriod {

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

  //-------------------------------------------------------------------------
  /**
   * Collects all the indices referred to by this period.
   * <p>
   * A period will typically refer to at least one index, such as 'GBP-LIBOR-3M'.
   * Each index that is referred to must be added to the specified builder.
   * 
   * @param builder  the builder to use
   */
  public abstract void collectIndices(ImmutableSet.Builder<Index> builder);

  /**
   * Adjusts the payment date using the rules of the specified adjuster.
   * <p>
   * The adjuster is typically an instance of {@link BusinessDayAdjustment}.
   * Implementations must return a new instance unless they are immutable and no change occurs.
   * 
   * @param adjuster  the adjuster to apply to the payment date
   * @return the adjusted payment event
   */
  SwapPaymentPeriod adjustPaymentDate(TemporalAdjuster adjuster);

}
