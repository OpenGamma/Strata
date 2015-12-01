/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing a rate swap.
 * <p>
 * A rate swap is a financial instrument that represents the exchange of streams of payments.
 * The swap is formed of legs, where each leg typically represents the obligations
 * of the seller or buyer of the swap. In the simplest vanilla interest rate swap,
 * there are two legs, one with a fixed rate and the other a floating rate.
 * Many other more complex swaps can also be represented.
 * <p>
 * For example, a swap might involve an agreement to exchange the difference between
 * the fixed rate of 1% and the 'GBP-LIBOR-3M' rate every 3 months for 2 years.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface SwapProduct
    extends Product, Expandable<ExpandedSwap> {

}
