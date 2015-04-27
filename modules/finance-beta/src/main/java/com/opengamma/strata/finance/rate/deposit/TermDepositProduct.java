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
 * A product representing a term deposit.
 * <p>
 * A term deposit is a financial instrument that provides a fixed rate of interest on
 * an amount for a specific term.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface TermDepositProduct
    extends Product, Expandable<ExpandedTermDeposit>, ImmutableBean {

}
