/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.bond;

import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.Product;

/**
 * A fixed coupon bond.
 * <p>
 * A fixed coupon bond is a financial instrument that represents a stream of fixed payments. 
 * The payments consist two types: periodic coupon payments and nominal payment.
 * <p>
 * The periodic payments are made {@code n} times a year with a fixed coupon rate at individual coupon dates.   
 * The nominal payment is the unique payment at the final coupon date.
 */
public interface FixedCouponBondProduct
    extends Product, Expandable<ExpandedFixedCouponBond> {

}
