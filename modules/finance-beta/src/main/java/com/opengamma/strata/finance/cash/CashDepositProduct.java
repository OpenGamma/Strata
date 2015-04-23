/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.cash;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;

/**
 * A product representing a cash deposit.
 * <p>
 * A cash deposit is a financial instrument that provides a fixed rate of interest on
 * an amount for a specific term.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface CashDepositProduct
    extends Product, Expandable<CashDeposit>, ImmutableBean {

}
