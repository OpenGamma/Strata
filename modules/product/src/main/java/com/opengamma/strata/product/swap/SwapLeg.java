/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.time.LocalDate;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;

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
public interface SwapLeg {

  /**
   * Gets the type of the leg, such as Fixed or Ibor.
   * <p>
   * This provides a high level categorization of the swap leg.
   * 
   * @return the leg type
   */
  public abstract SwapLegType getType();

  /**
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * 
   * @return the pay receive flag
   */
  public abstract PayReceive getPayReceive();

  /**
   * Gets the start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the leg
   */
  public abstract LocalDate getStartDate();

  /**
   * Gets the end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the leg
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

  //-------------------------------------------------------------------------
  /**
   * Returns the set of indices referred to by the leg.
   * <p>
   * A leg will typically refer to at least one index, such as 'GBP-LIBOR-3M'.
   * Calling this method will return the complete list of indices, including
   * any associated with FX reset.
   * 
   * @return the set of indices referred to by this leg
   */
  public default ImmutableSet<Index> allIndices() {
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    collectIndices(builder);
    return builder.build();
  }

  /**
   * Collects all the indices referred to by this leg.
   * <p>
   * A swap leg will typically refer to at least one index, such as 'GBP-LIBOR-3M'.
   * Each index that is referred to must be added to the specified builder.
   * 
   * @param builder  the builder to use
   */
  public abstract void collectIndices(ImmutableSet.Builder<Index> builder);

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
