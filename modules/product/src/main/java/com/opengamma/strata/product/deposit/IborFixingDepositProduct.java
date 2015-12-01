/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A product representing a Ibor fixing deposit.
 * <p>
 * An Ibor fixing deposit is a fictitious financial instrument that represents the fixing based on Ibor-like index. 
 * <p>
 * For example, an Ibor fixing deposit involves the exchange of the difference between
 * the fixed rate of 1% and the 'GBP-LIBOR-3M' rate for the principal in 3 months time.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface IborFixingDepositProduct
    extends Product, Expandable<ExpandedIborFixingDeposit> {

}
