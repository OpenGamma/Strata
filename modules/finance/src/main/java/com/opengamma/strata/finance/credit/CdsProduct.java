/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;

/**
 * A product representing a credit default swap.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface CdsProduct
    extends Product, Expandable<ExpandedCds>, ImmutableBean {

}
