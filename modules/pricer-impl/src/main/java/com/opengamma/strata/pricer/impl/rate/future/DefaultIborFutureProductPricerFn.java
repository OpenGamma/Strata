/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.future;

import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.future.IborFutureProductPricerFn;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer implementation for Ibor future products.
 */
public class DefaultIborFutureProductPricerFn
    implements IborFutureProductPricerFn {

  /**
   * Default implementation.
   */
  public static final DefaultIborFutureProductPricerFn DEFAULT = new DefaultIborFutureProductPricerFn();

  /**
   * Creates an instance.
   */
  public DefaultIborFutureProductPricerFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(PricingEnvironment env, IborFuture future) {
    double forward = env.iborIndexRate(future.getIndex(), future.getLastTradeDate());
    // TODO: Marc: getting the rounding so deep in the code will cause problem. We will need two definition of the
    //  IborFuture, one without rounding, to be able to do standard computation like finite difference sensitivity 
    //  computation or scenario analysis.
    return future.getRounding().round(1.0 - forward);
  }

  @Override
  public PointSensitivities priceSensitivity(PricingEnvironment env, IborFuture future) {
    IborRateSensitivity sensi = IborRateSensitivity.of(future.getIndex(), future.getLastTradeDate(), -1.0d);
    // The sensitivity should be to no currency or currency XXX. To avoid useless conversion, the dimension-less 
    // price sensitivity is reported in the future currency.
    return PointSensitivities.of(sensi);
  }

}
