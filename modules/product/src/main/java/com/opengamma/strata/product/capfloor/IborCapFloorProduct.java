/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing an Ibor cap/floor. 
 * <p>
 * The Ibor cap/floor product consists of two legs, a cap/floor leg and a pay leg.
 * The cap/floor leg involves a set of call/put options on successive Ibor index rates,
 * known as Ibor caplets/floorlets.
 * The pay leg is any swap leg from a standard interest rate swap. The pay leg is absent for typical
 * Ibor cap/floor products, with the premium paid upfront instead, as defined in {@link IborCapFloorTrade}.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface IborCapFloorProduct
    extends Product, Expandable<ExpandedIborCapFloor> {

}
