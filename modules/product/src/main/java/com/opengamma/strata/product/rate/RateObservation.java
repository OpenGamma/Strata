/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Defines a mechanism for observing an interest rate.
 * <p>
 * A floating rate can be observed in a number of ways, including from one index,
 * interpolating two indices, averaging an index on specific dates, overnight compounding
 * and overnight averaging.
 * <p>
 * Each implementation contains the necessary information to observe the rate.
 * <p>
 * This is a marker interface, see implementations such as {@link IborRateObservation} and
 * {@link OvernightCompoundedRateObservation} for more information.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface RateObservation {

  /**
   * Collects all the indices referred to by this observation.
   * <p>
   * An observation will typically refer to one index, such as 'GBP-LIBOR-3M'.
   * Each index that is referred to must be added to the specified builder.
   * 
   * @param builder  the builder to use
   */
  public abstract void collectIndices(ImmutableSet.Builder<Index> builder);

}
