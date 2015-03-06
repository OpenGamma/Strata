/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import java.time.LocalDate;

import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.Trade;
import com.opengamma.platform.pricer.PricingEnvironment;
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
  
  public MultipleCurrencyParameterSensitivity calculateSensitivity(PricingEnvironment env, LocalDate valuationDate, Trade trade) {
    MultiCurrencyAmount pvInit = tradePricer.presentValue(env, trade);
    return null;
  }
  

}
