/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.product.common.PayReceive;

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
 * required by this interface. The {@link ResolvedSwapLeg} class, which this leg can
 * be converted to, does define the swap in terms of payment periods.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface SwapLeg extends Resolvable<ResolvedSwapLeg> {

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
   * Gets the accrual start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * <p>
   * Defined as the effective date by the 2006 ISDA definitions article 3.2.
   * 
   * @return the start date of the leg
   */
  public abstract AdjustableDate getStartDate();

  /**
   * Gets the accrual end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the termination date.
   * <p>
   * Defined as the termination date by the 2006 ISDA definitions article 3.3.
   * 
   * @return the end date of the leg
   */
  public abstract AdjustableDate getEndDate();

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
   * Resolves this swap leg using the specified reference data.
   * <p>
   * This converts the swap leg to the equivalent resolved form.
   * All {@link ReferenceDataId} identifiers in this instance will be resolved.
   * The resolved form will typically be a type that is optimized for pricing.
   * <p>
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data to use when resolving
   * @return the resolved instance
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid definition
   */
  @Override
  public abstract ResolvedSwapLeg resolve(ReferenceData refData);

}
