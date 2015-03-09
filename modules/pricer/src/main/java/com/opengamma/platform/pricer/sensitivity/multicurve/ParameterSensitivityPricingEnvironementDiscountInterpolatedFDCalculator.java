/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity.multicurve;

import com.opengamma.platform.finance.Trade;
import com.opengamma.platform.pricer.TradePricerFn;

public class ParameterSensitivityPricingEnvironementDiscountInterpolatedFDCalculator {
  
  /** The pricer user for the present value. */
  private final TradePricerFn<Trade> tradePricer;  
  /** The shift used for finite difference. */
  private final double shift;
  
  public ParameterSensitivityPricingEnvironementDiscountInterpolatedFDCalculator(TradePricerFn<Trade> tradePricer, double shift) {
    this.tradePricer = tradePricer;
    this.shift = shift;
  }
  
  // MultipleCurrencyParameterSensitivity in 2.x
  //  public MultipleCurrencyParameterSensitivity calculateSensitivity(PricingEnvironment env, LocalDate valuationDate, Trade trade) {
  //    MultiCurrencyAmount pvInit = tradePricer.presentValue(env, trade);
  //    return null;
  //  }
  

}
