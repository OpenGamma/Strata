/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.equity;

import org.joda.beans.ImmutableBean;

import com.opengamma.platform.finance.Product;

/**
 * A product representing an equity share of a company.
 * <p>
 * This represents the concept of a single equity share of a company.
 * For example, a single share of OpenGamma.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface EquityProduct
    extends Product, ImmutableBean {

}
