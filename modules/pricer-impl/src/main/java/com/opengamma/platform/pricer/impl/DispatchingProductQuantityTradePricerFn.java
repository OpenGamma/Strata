/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.Product;
import com.opengamma.platform.finance.QuantityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;

/**
 * Pricer implementation for quantity trades using multiple dispatch.
 * <p>
 * Extracts the product from the quantity trade and dispatches the request to the correct implementation.
 */
public class DispatchingProductQuantityTradePricerFn
    implements TradePricerFn<QuantityTrade<?>> {

  /**
   * Default implementation.
   */
  public static final DispatchingProductQuantityTradePricerFn DEFAULT = new DispatchingProductQuantityTradePricerFn();

  /**
   * Creates an instance.
   */
  public DispatchingProductQuantityTradePricerFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, QuantityTrade<?> trade) {
    Product product = trade.getSecurity().getProduct();
    // dispatch by runtime type
    throw new IllegalArgumentException("Unknown Product type: " + product.getClass().getSimpleName());
  }

}
