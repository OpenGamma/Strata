/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.swap.SwapIndex;

/**
 * A product representing a constant maturity swap (CMS) or CMS cap/floor. 
 * <p>
 * The CMS product consists of two legs, a CMS leg and a pay leg.
 * The CMS leg of CMS periodically pays coupons based on swap rate, which is the observed
 * value of a {@linkplain SwapIndex swap index}.
 * The pay leg is any swap leg from a standard interest rate swap. The pay leg may be absent
 * for certain CMS products, with the premium paid upfront instead, as defined on {@link CmsTrade}.
 * <p>
 * For example, a CMS trade might involve an agreement to exchange the difference between
 * the fixed rate of 1% and the swap rate of 5-year 'GBP-FIXED-6M-LIBOR-6M' swaps every 6 months for 2 years.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface CmsProduct
    extends Product, Expandable<ExpandedCms> {

}
