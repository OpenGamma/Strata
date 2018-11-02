/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import com.opengamma.strata.pricer.impl.tree.AmericanVanillaOptionFunction;
import com.opengamma.strata.pricer.impl.tree.OptionFunction;
import com.opengamma.strata.product.common.PutCall;

public class AmericanOptionWithDiscreteDividends extends AmericanOption implements Option {
  private double[] dividendTimes;
  private double[] dividendAmounts;
  private OptionFunction americanVanillaOptionFunction;
  
  AmericanOptionWithDiscreteDividends(
      double quantity,
      double multiplier,
      double strike,
      double expiry,
      PutCall putCall,
      double[] dividendTimes,
      double[] dividendAmounts){
    super(
        quantity,
        multiplier,
        strike,
        expiry,
        putCall);
    this.dividendTimes = dividendTimes;
    this.dividendAmounts = dividendAmounts;
    this.americanVanillaOptionFunction = AmericanVanillaOptionFunction.of(strike, expiry, putCall, STEPS);
  }
  
  public double calculate(double spot, double rate, double vol){
    return BINOMIAL_TREE.optionPrice(americanVanillaOptionFunction, LATTICE, spot, vol, rate, dividendAmounts, dividendTimes);
  }
}
