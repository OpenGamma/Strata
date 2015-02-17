/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.future;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.future.ExpandedIborFuture;
import com.opengamma.platform.finance.future.IborFuture;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureProductPricerFn;

/**
 * Pricer implementation for swap products.
 * <p>
 * The swap product is priced by by expanding it.
 */
public class ExpandingIborFutureProductPricerFn
    implements IborFutureProductPricerFn<IborFuture> {

  /**
   * Default implementation.
   */
  public static final ExpandingIborFutureProductPricerFn DEFAULT = new ExpandingIborFutureProductPricerFn(
      DefaultExpandedIborFuturePricerFn.DEFAULT);

  /**
   * Pricer for {@link ExpandedIborFuture}.
   */
  private final IborFutureProductPricerFn<ExpandedIborFuture> expandedIborFuturePricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedIborFuturePricerFn  the pricer for {@link ExpandedIborFuture}
   */
  public ExpandingIborFutureProductPricerFn(
      IborFutureProductPricerFn<ExpandedIborFuture> expandedIborFuturePricerFn) {
    this.expandedIborFuturePricerFn = ArgChecker.notNull(expandedIborFuturePricerFn, "expandedIborFuturePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(PricingEnvironment env, IborFuture iborFutureProduct) {
    return expandedIborFuturePricerFn.price(env, iborFutureProduct.expand());
  }

}
