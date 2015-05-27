/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;

/**
 * A product representing a Ibor fixing deposit.
 * <p>
 * An Ibor fixing deposit is a fictitious financial instrument that represents the fixing based on Ibor-like index. 
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface IborFixingDepositProduct
    extends Product, Expandable<ExpandedIborFixingDeposit>, ImmutableBean {

}
