/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.OtcTrade;
import com.opengamma.platform.finance.QuantityTrade;
import com.opengamma.platform.finance.Trade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;

/**
 * Pricer implementation for trades using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingTradePricerFn
    implements TradePricerFn<Trade> {

  /**
   * Default implementation.
   */
  public static final DispatchingTradePricerFn DEFAULT = new DispatchingTradePricerFn(
      DispatchingProductOtcTradePricerFn.DEFAULT,
      DispatchingProductQuantityTradePricerFn.DEFAULT);

  /**
   * Pricer for {@link OtcTrade}.
   */
  private final TradePricerFn<OtcTrade<?>> otcTradePricerFn;
  /**
   * Pricer for {@link QuantityTrade}.
   */
  private final TradePricerFn<QuantityTrade<?>> quantityTradePricerFn;

  /**
   * Creates an instance.
   * 
   * @param otcTradePricerFn  the pricer for {@link OtcTrade}
   * @param quantityTradePricerFn  the pricer for {@link QuantityTrade}
   */
  public DispatchingTradePricerFn(
      TradePricerFn<OtcTrade<?>> otcTradePricerFn,
      TradePricerFn<QuantityTrade<?>> quantityTradePricerFn) {
    this.otcTradePricerFn = ArgChecker.notNull(otcTradePricerFn, "otcTradePricerFn");
    this.quantityTradePricerFn = ArgChecker.notNull(quantityTradePricerFn, "quantityTradePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, Trade trade) {
    // dispatch by runtime type
    if (trade instanceof OtcTrade) {
      return otcTradePricerFn.presentValue(env, (OtcTrade<?>) trade);
    } else if (trade instanceof QuantityTrade) {
      return quantityTradePricerFn.presentValue(env, (QuantityTrade<?>) trade);
    } else {
      throw new IllegalArgumentException("Unknown Trade type: " + trade.getClass().getSimpleName());
    }
  }

}
