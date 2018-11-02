/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import com.opengamma.strata.pricer.impl.option.BlackScholesFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

public class EuropeanOptionWithContinuousYield extends EuropeanOption implements Option{
  
  private double continuousYield;
  
  EuropeanOptionWithContinuousYield(
      double quantity,
      double notional,
      double strike,
      double expiry,
      PutCall putCall,
      double continuousYield){
    super(
        quantity,
        notional,
        strike,
        expiry,
        putCall );
    this.continuousYield = continuousYield;
  }
  
  public double calculate(double spot, double rate, double vol){
    return BlackScholesFormulaRepository.price(spot, strike(), expiry(), vol, rate, continuousYield, putCall().isCall());
  }
}
