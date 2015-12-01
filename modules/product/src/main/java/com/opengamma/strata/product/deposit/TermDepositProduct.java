/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing a term deposit.
 * <p>
 * A term deposit is a financial instrument that provides a fixed rate of interest on
 * an amount for a specific term.
 * For example, investing GBP 1,000 for 3 months at a 1% interest rate.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface TermDepositProduct
    extends Product, Expandable<ExpandedTermDeposit> {

}
