/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import com.opengamma.strata.product.Expandable;
import com.opengamma.strata.product.Product;

/**
 * A capital indexed bond.
 * <p>
 * A capital indexed bond is a financial instrument that represents a stream of inflation-adjusted payments. 
 * The payments consist two types: periodic coupon payments and nominal payment.
 * <p>
 * The periodic payments are made {@code n} times a year with a real coupon rate at individual coupon dates.   
 * The nominal payment is the unique payment at the final coupon date.
 * All of the payments are adjusted for inflation. 
 */
public interface CapitalIndexedBondProduct
    extends Product, Expandable<ExpandedCapitalIndexedBond> {

}
