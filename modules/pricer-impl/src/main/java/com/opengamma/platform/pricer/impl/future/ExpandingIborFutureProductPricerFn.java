/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.future;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.future.ExpandedIborFuture;
import com.opengamma.platform.finance.future.IborFuture;
import com.opengamma.platform.finance.future.IborFutureSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureProductPricerFn;

/**
 * Pricer implementation for Ibor future products.
 * <p>
 * The product is priced by expanding it.
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
  private final IborFutureProductPricerFn<ExpandedIborFuture> expandedFuturePricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedFuturePricerFn  the pricer for {@link ExpandedIborFuture}
   */
  public ExpandingIborFutureProductPricerFn(
      IborFutureProductPricerFn<ExpandedIborFuture> expandedFuturePricerFn) {
    this.expandedFuturePricerFn = ArgChecker.notNull(expandedFuturePricerFn, "expandedFuturePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(PricingEnvironment env, IborFuture iborFuture) {
    return expandedFuturePricerFn.price(env, iborFuture.expand());
  }

  @Override
  public CurrencyAmount presentValue(
      PricingEnvironment env,
      IborFuture iborFuture,
      IborFutureSecurityTrade trade,
      double lastClosingPrice) {

    return expandedFuturePricerFn.presentValue(env, iborFuture.expand(), trade, lastClosingPrice);
  }

}
