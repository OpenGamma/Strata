/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing a foreign exchange swap.
 * <p>
 * An FX swap is a financial instrument that represents the exchange of an equivalent amount
 * in two different currencies between counterparties on two different dates.
 * <p>
 * For example, an FX swap might represent the payment of USD 1,000 and the receipt of EUR 932
 * on one date, and the payment of EUR 941 and the receipt of USD 1,000 at a later date.
 * <p>
 * Application code should use {@link FxSwap}.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface FxSwapProduct
    extends Product, Expandable<ExpandedFxSwap> {

}
