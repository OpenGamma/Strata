/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import java.time.LocalDate;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.Trade;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;
import com.opengamma.platform.pricer.impl.swap.StandardSwapTradePricerFn;

/**
 * Multiple dispatch for {@code AccrualPeriodPricerFn}.
 * <p>
 * Dispatches the pricer request to the correct implementation.
 */
public class StandardTradePricerFn
    implements TradePricerFn<Trade> {

  /**
   * Default implementation.
   */
  public static final StandardTradePricerFn DEFAULT = new StandardTradePricerFn(
      StandardSwapTradePricerFn.DEFAULT);

  //-------------------------------------------------------------------------
  /**
   * Handle {@link SwapTrade}.
   */
  private TradePricerFn<SwapTrade> swapTradePricerFn;

  /**
   * Creates an instance.
   * 
   * @param swapTradePricerFn  the rate provider for {@link SwapTrade}
   */
  public StandardTradePricerFn(
      TradePricerFn<SwapTrade> swapTradePricerFn) {
    super();
    this.swapTradePricerFn = ArgChecker.notNull(swapTradePricerFn, "swapTradePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      Trade trade) {
    // dispatch by runtime type
    if (trade instanceof SwapTrade) {
      return swapTradePricerFn.presentValue(env, valuationDate, (SwapTrade) trade);
    } else {
      throw new IllegalArgumentException("Unknown Trade type: " + trade.getClass().getSimpleName());
    }
  }

  @Override
  public MultiCurrencyAmount futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      Trade trade) {
    // dispatch by runtime type
    if (trade instanceof SwapTrade) {
      return swapTradePricerFn.futureValue(env, valuationDate, (SwapTrade) trade);
    } else {
      throw new IllegalArgumentException("Unknown Trade type: " + trade.getClass().getSimpleName());
    }
  }

}
