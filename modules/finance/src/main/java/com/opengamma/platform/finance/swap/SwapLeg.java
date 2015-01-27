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
 * A single leg of a swap.
 * <p>
 * A swap leg is one element of a {@link Swap}.
 * In most cases, a swap has two legs, one expressing the obligations of the seller
 * and one expressing the obligations of the buyer. However, it is possible to
 * represent more complex swaps, with one, three or more legs.
 * <p>
 * This interface imposes few restrictions on the swap leg. A leg must have a start and
 * end date, where the start date can be before or after the date that the swap is traded.
 * A single swap leg must produce payments in a single currency.
 * <p>
 * In most cases, a swap will consist of a list of payment periods, but this is not
 * required by this interface. The {@link ExpandedSwapLeg} class, which this leg can
 * be converted to, does define the swap in terms of payment periods.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface SwapLeg
    extends ImmutableBean {

  /**
   * Gets the start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  public abstract LocalDate getStartDate();

  /**
   * Gets the end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the period
   */
  public abstract LocalDate getEndDate();

  /**
   * Gets the currency of the leg.
   * <p>
   * A swap leg has a single currency.
   * 
   * @return the currency of the leg
   */
  public abstract Currency getCurrency();

  /**
   * Expands this swap leg.
   * <p>
   * Expanding a swap leg causes the dates to be adjusted according to the relevant
   * holiday calendar. Other one-off calculations may also be performed.
   * 
   * @return the expended swap leg
   * @throws RuntimeException if unable to expand
   */
  public abstract ExpandedSwapLeg expand();

}
