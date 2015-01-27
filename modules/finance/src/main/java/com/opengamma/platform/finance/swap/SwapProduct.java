/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import org.joda.beans.ImmutableBean;

import com.opengamma.platform.finance.Product;

/**
 * A swap product that can be traded.
 * <p>
 * A swap product is a financial instrument that represents the exchange of streams of payments.
 * The swap is formed of legs, where each leg typically represents the obligations
 * of the seller or buyer of the swap. In the simplest vanilla interest rate swap,
 * there are two legs, one with a fixed rate and the other a floating rate.
 * Many other more complex swaps can also be represented.
 * <p>
 * An instance of {@code SwapProduct} can exist independently from a {@link SwapTrade}.
 * This would occur if the swap has not actually been traded, such as the underlying on a swaption.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface SwapProduct
    extends Product<ExpandedSwap>, ImmutableBean {

}
