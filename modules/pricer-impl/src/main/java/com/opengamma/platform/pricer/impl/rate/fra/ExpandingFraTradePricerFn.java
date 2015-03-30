/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.fra;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.rate.fra.ExpandedFra;
import com.opengamma.platform.finance.rate.fra.FraTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.rate.fra.FraTradePricerFn;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Pricer implementation for forward rate agreement (FRA) products.
 * <p>
 * The forward rate agreement product is priced by by expanding it.
 */
public class ExpandingFraTradePricerFn
    implements FraTradePricerFn {

  /**
   * Default implementation.
   */
  public static final ExpandingFraTradePricerFn DEFAULT = new ExpandingFraTradePricerFn(
      DiscountingExpandedFraPricerFn.DEFAULT);

  /**
   * Pricer for {@link ExpandedFra}.
   */
  private final FraProductPricerFn<ExpandedFra> expandedFraPricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedFraPricerFn  the pricer for {@link ExpandedFra}
   */
  public ExpandingFraTradePricerFn(
      FraProductPricerFn<ExpandedFra> expandedFraPricerFn) {
    this.expandedFraPricerFn = ArgChecker.notNull(expandedFraPricerFn, "expandedFraPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, FraTrade trade) {
    return expandedFraPricerFn.presentValue(env, trade.getProduct().expand());
  }

  @Override
  public CurrencyAmount futureValue(PricingEnvironment env, FraTrade trade) {
    return expandedFraPricerFn.futureValue(env, trade.getProduct().expand());
  }

}
