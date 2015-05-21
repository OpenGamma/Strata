/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;
import org.joda.beans.ImmutableBean;

/**
 * A product representing a credit default swap.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface CreditDefaultSwapProduct
    extends Product, Expandable<ExpandedCreditDefaultSwap>, ImmutableBean {
}
