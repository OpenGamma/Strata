/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.future;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.pricer.rate.future.IborFutureProductPricerFn;
import com.opengamma.strata.pricer.rate.future.IborFutureTradePricerFn;

/**
 * Pricer implementation for Ibor future trades.
 */
public class DefaultIborFutureTradePricerFn
    implements IborFutureTradePricerFn {

  /**
   * Default implementation.
   */
  public static final DefaultIborFutureTradePricerFn DEFAULT =
      new DefaultIborFutureTradePricerFn(DefaultIborFutureProductPricerFn.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final IborFutureProductPricerFn futurePricerFn;

  /**
   * Creates an instance.
   * 
   * @param futurePricerFn  the pricer for {@link IborFuture}
   */
  public DefaultIborFutureTradePricerFn(
      IborFutureProductPricerFn futurePricerFn) {
    this.futurePricerFn = ArgChecker.notNull(futurePricerFn, "futurePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFutureProductPricerFn getFutureProductPricerFn() {
    return futurePricerFn;
  }
  
}
