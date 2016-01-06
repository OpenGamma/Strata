/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * A constant maturity swap (CMS) or CMS cap/floor. 
 * <p>
 * The CMS product consists of two legs: CMS leg and pay leg. 
 * The CMS leg of CMS periodically pays coupons based on swap rate, the observed value of {@linkplain SwapIndex swap index},  
 * CMS cap/floor is a set of call/put options on successive swap rates, i.e., CMS caplets/floorlets. 
 * The other leg is typically the same as a swap leg of the standard interest rate swap. See {@link SwapLeg}.
 * <p>
 * However, the pay leg is absent for certain CMS products. Instead the premium is paid upfront. See {@link CmsTrade}.
 */
public interface CmsProduct
    extends Product, Expandable<ExpandedCms> {

}
