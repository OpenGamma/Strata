/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.equity;

import com.opengamma.strata.product.Product;

/**
 * A product representing an equity share of a company.
 * <p>
 * This represents the concept of a single equity share of a company.
 * For example, a single share of OpenGamma.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface EquityProduct
    extends Product {

}
