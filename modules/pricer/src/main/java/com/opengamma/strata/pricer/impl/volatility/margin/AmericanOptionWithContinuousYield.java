/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import com.opengamma.strata.pricer.impl.tree.AmericanVanillaOptionFunction;
import com.opengamma.strata.pricer.impl.tree.OptionFunction;
import com.opengamma.strata.product.common.PutCall;

public class AmericanOptionWithContinuousYield extends AmericanOption implements Option{
  
  private double continuousYield;
  private OptionFunction americanVanillaOptionFunction;
  
  AmericanOptionWithContinuousYield(
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
    this.americanVanillaOptionFunction = AmericanVanillaOptionFunction.of(strike, expiry, putCall, STEPS);
  }
  
  public double calculate(double spot, double rate, double vol){
    return TRINOMIAL_TREE.optionPrice(americanVanillaOptionFunction, LATTICE, spot, vol, rate, continuousYield);
  }
}
