/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.fra;

import org.joda.beans.ImmutableBean;

import com.opengamma.platform.finance.Product;

/**
 * A forward rate agreement (FRA) product that can be traded.
 * <p>
 * A FRA is a financial instrument that represents the one off exchange of a fixed
 * rate of interest for a floating rate.
 * <p>
 * An instance of {@code FraProduct} can exist independently from a {@link FraTrade}.
 * This would occur if the FRA has not actually been traded.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface FraProduct
    extends Product<ExpandedFra>, ImmutableBean {

}
