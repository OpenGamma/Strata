/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.fra;

import com.opengamma.platform.finance.rate.fra.ExpandedFra;
import com.opengamma.platform.finance.rate.fra.FraProduct;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;

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
  public CurrencyAmount presentValue(PricingEnvironment env, FraProduct product) {
    return expandedFraPricerFn.presentValue(env, product.expand());
  }

  @Override
  public PointSensitivities presentValueSensitivity(PricingEnvironment env, FraProduct product) {
    return expandedFraPricerFn.presentValueSensitivity(env, product.expand());
  }

  @Override
  public CurrencyAmount futureValue(PricingEnvironment env, FraProduct product) {
    return expandedFraPricerFn.futureValue(env, product.expand());
  }

  @Override
  public PointSensitivities futureValueSensitivity(PricingEnvironment env, FraProduct product) {
    return expandedFraPricerFn.futureValueSensitivity(env, product.expand());
  }

}
