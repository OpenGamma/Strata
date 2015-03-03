/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.future;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.future.ExpandedIborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureOptionProductPricerFn;

/**
 * Pricer implementation for Ibor future option products.
 * <p>
 * The product is priced by expanding it.
 */
public class ExpandingIborFutureOptionProductPricerFn
    implements IborFutureOptionProductPricerFn<IborFutureOption> {

  /**
   * Default implementation.
   */
  public static final ExpandingIborFutureOptionProductPricerFn DEFAULT = new ExpandingIborFutureOptionProductPricerFn(
      NormalExpandedIborFutureOptionPricerFn.DEFAULT);

  /**
   * Pricer for {@link ExpandedIborFutureOption}.
   */
  private final IborFutureOptionProductPricerFn<ExpandedIborFutureOption> expandedOptionPricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedOptionPricerFn  the pricer for {@link ExpandedIborFutureOption}
   */
  public ExpandingIborFutureOptionProductPricerFn(
      IborFutureOptionProductPricerFn<ExpandedIborFutureOption> expandedOptionPricerFn) {
    this.expandedOptionPricerFn = ArgChecker.notNull(expandedOptionPricerFn, "expandedOptionPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(PricingEnvironment env, IborFutureOption iborFutureOptionProduct, Object surface) {
    return expandedOptionPricerFn.price(env, iborFutureOptionProduct.expand(), surface);
  }

  @Override
  public CurrencyAmount presentValue(
      PricingEnvironment env,
      IborFutureOption iborFutureOptionProduct,
      IborFutureOptionSecurityTrade trade,
      double lastClosingPrice,
      Object surface) {

    return expandedOptionPricerFn.presentValue(env, iborFutureOptionProduct.expand(), trade, lastClosingPrice, surface);
  }

}
