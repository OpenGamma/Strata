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
 * A product representing an option on futures contract based on an IBOR-like index.
 * <p>
 * An Ibor future option is an option on a future value of
 * an IBOR-like interest rate.
 * An Ibor future option is also known as a <i>STIR future option</i>.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface IborFutureOptionProduct
    extends Product, Expandable<ExpandedIborFutureOption>, ImmutableBean {

}
