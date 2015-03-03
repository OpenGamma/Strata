/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.future;

import org.joda.beans.ImmutableBean;

import com.opengamma.platform.finance.Expandable;
import com.opengamma.platform.finance.Product;

/**
 * A product representing a futures contract based on an IBOR-like index.
 * <p>
 * An Ibor future is a financial instrument that is based on the future value of
 * an IBOR-like interest rate.
 * An Ibor future is also known as a <i>STIR future</i> (Short Term Interest Rate).
 * <p>
 * For example, the widely traded "CME Eurodollar futures contract" has a notional
 * of 1 million USD, is based on the USD Libor 3 month rate 'USD-LIBOR-3M' and expires
 * on the quarterly IMM date, the 3rd Wednesday of March, June, September and December.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface IborFutureProduct
    extends Product, Expandable<ExpandedIborFuture>, ImmutableBean {

}
