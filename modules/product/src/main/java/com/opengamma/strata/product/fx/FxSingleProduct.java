/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing a simple foreign exchange between two counterparties.
 * <p>
 * This represents a single foreign exchange on a specific date.
 * For example, it might represent the payment of USD 1,000 and the receipt of EUR 932.
 * <p>
 * An FX forward and an FX spot can be represented using this product.
 * Application code should use {@link FxSingle}.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface FxSingleProduct
    extends Product, Expandable<ExpandedFxSingle> {

}
