/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.fra.ExpandedFra;
import com.opengamma.platform.finance.fra.FraProduct;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;

/**
 * Pricer implementation for forward rate agreement (FRA) products.
 * <p>
 * The forward rate agreement product is priced by by expanding it.
 */
public class ExpandingFraProductPricerFn
    implements FraProductPricerFn<FraProduct> {

  /**
   * Default implementation.
   */
  public static final ExpandingFraProductPricerFn DEFAULT = new ExpandingFraProductPricerFn(
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
  public ExpandingFraProductPricerFn(
      FraProductPricerFn<ExpandedFra> expandedFraPricerFn) {
    this.expandedFraPricerFn = ArgChecker.notNull(expandedFraPricerFn, "expandedFraPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, FraProduct product) {
    return expandedFraPricerFn.presentValue(env, product.expand());
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, FraProduct product) {
    return expandedFraPricerFn.futureValue(env, product.expand());
  }

}
