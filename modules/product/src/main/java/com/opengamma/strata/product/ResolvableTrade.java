/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.Resolvable;

/**
 * A trade that can to be resolved using reference data.
 * <p>
 * Resolvable trades are the primary definition of a trade that applications work with.
 * They are resolved when necessary for use with pricers, locking in specific reference data.
 * 
 * @param <T>  the type of the resolved trade
 */
public interface ResolvableTrade<T extends ResolvedTrade>
    extends Trade, Resolvable<T> {

  /**
   * Resolves this trade using the specified reference data.
   * <p>
   * This converts this trade to the equivalent resolved form.
   * All {@link ReferenceDataId} identifiers in this instance will be resolved.
   * The resulting {@link ResolvedTrade} is optimized for pricing.
   * <p>
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data to use when resolving
   * @return the resolved trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid definition
   */
  @Override
  public abstract T resolve(ReferenceData refData);

}
