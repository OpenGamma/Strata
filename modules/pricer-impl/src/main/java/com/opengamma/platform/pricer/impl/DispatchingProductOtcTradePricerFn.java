/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.OtcTrade;
import com.opengamma.platform.finance.Product;
import com.opengamma.platform.finance.fra.FraProduct;
import com.opengamma.platform.finance.swap.SwapProduct;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.impl.fra.ExpandingFraProductPricerFn;
import com.opengamma.platform.pricer.impl.swap.ExpandingSwapProductPricerFn;
import com.opengamma.platform.pricer.swap.SwapProductPricerFn;

/**
 * Pricer implementation for OTC trades using multiple dispatch.
 * <p>
 * Extracts the product from the OTC trade and dispatches the request to the correct implementation.
 */
public class DispatchingProductOtcTradePricerFn
    implements TradePricerFn<OtcTrade<?>> {

  /**
   * Default implementation.
   */
  public static final DispatchingProductOtcTradePricerFn DEFAULT = new DispatchingProductOtcTradePricerFn(
      ExpandingSwapProductPricerFn.DEFAULT,
      ExpandingFraProductPricerFn.DEFAULT);

  /**
   * Pricer for {@link SwapProduct}.
   */
  private final SwapProductPricerFn<SwapProduct> swapPricerFn;
  /**
   * Pricer for {@link FraProduct}.
   */
  private final FraProductPricerFn<FraProduct> fraPricerFn;

  /**
   * Creates an instance.
   * 
   * @param swapPricerFn  the pricer for {@link SwapProduct}
   * @param fraPricerFn  the pricer for {@link FraProduct}
   */
  public DispatchingProductOtcTradePricerFn(
      SwapProductPricerFn<SwapProduct> swapPricerFn,
      FraProductPricerFn<FraProduct> fraPricerFn) {
    this.swapPricerFn = ArgChecker.notNull(swapPricerFn, "swapPricerFn");
    this.fraPricerFn = ArgChecker.notNull(fraPricerFn, "fraPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, OtcTrade<?> trade) {
    Product product = trade.getProduct();
    // dispatch by runtime type
    if (product instanceof SwapProduct) {
      return swapPricerFn.presentValue(env, (SwapProduct) product);
    } else if (product instanceof FraProduct) {
      return fraPricerFn.presentValue(env, (FraProduct) product);
    } else {
      throw new IllegalArgumentException("Unknown Product type: " + product.getClass().getSimpleName());
    }
  }

}
