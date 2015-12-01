/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing an option on an underlying swap.
 * <p>
 * A swaption is a financial instrument that provides an option based on the future value of a swap.
 * The option is European, exercised only on the exercise date.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface SwaptionProduct
    extends Product, Expandable<ExpandedSwaption> {

}
