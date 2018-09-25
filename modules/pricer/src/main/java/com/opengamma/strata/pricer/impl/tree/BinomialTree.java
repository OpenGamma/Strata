/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

public class BinomialTree {
    
  private static final double BUMP = 1.0e-6;

  public static double optionPrice(
      OptionFunction function,
      LatticeSpecification lattice,
      double spot,
      double volatility,
      double interestRate,
      double[] cashDividends,
      double[] dividendTimes) {
      
    int nSteps = function.getNumberOfSteps();
    double timeToExpiry = function.getTimeToExpiry();
    double dt = timeToExpiry / (double) nSteps;
    double discount = Math.exp(-interestRate * dt);
    DoubleArray params = lattice.getParametersBinomial(volatility, interestRate, dt);
    double upFactor = params.get(0);
    double downFactor = params.get(1);
    double upProbability = params.get(2);
    double downProbability = params.get(3);
    double pvDividends = 0;
    for(int i = 0; i < cashDividends.length; ++i)
      pvDividends += cashDividends[i] * Math.exp( -interestRate*dividendTimes[i]);
    double modifiedSpot = spot - pvDividends;
    ArgChecker.isTrue(upProbability > 0d, "upProbability should be greater than 0");
    ArgChecker.isTrue(upProbability < 1d, "upProbability should be smaller than 1");
    ArgChecker.isTrue(downProbability > 0d, "downProbability should be greater than 0");
    //Assume dividends before expiry
    DoubleArray values = function.getPayoffAtExpiryBinomial(modifiedSpot, upFactor, downFactor);
    for (int i = nSteps - 1; i > -1; --i) {
      double divs = 0.;
      for(int j = 0; j < cashDividends.length; ++j ){
        if(i * dt < dividendTimes[j]){
          divs += cashDividends[j] * Math.exp(-interestRate * (dividendTimes[j] - i * dt));
        }
      }
      values = function.getNextOptionValuesBinomial(discount, upProbability, downProbability, values, modifiedSpot,
          upFactor, downFactor, i, divs);
    }
    return values.get(0);
  }
    
  public static ValueDerivatives optionPriceAdjoint(
      OptionFunction function,
      LatticeSpecification lattice,
      double spot,
      double volatility,
      double interestRate,
      double[] cashDividends,
      double[] dividendTimes) {
      
    int nSteps = function.getNumberOfSteps();
    double timeToExpiry = function.getTimeToExpiry();
    double dt = timeToExpiry / (double) nSteps;
    double discount = Math.exp(-interestRate * dt);
    DoubleArray params = lattice.getParametersBinomial(volatility, interestRate, dt);
    double upFactor = params.get(0);
    double downFactor = params.get(1);
    double upProbability = params.get(2);
    double downProbability = params.get(3);
    double pvDividends = 0;
    for(int i = 0; i < cashDividends.length; ++i)
      pvDividends += cashDividends[i] * Math.exp( -interestRate*dividendTimes[i]);
    double modifiedSpot = spot - pvDividends;
    ArgChecker.isTrue(upProbability > 0d, "upProbability should be greater than 0");
    ArgChecker.isTrue(upProbability < 1d, "upProbability should be smaller than 1");
    ArgChecker.isTrue(downProbability > 0d, "downProbability should be greater than 0");
    DoubleArray values = function.getPayoffAtExpiryBinomial(modifiedSpot, upFactor, downFactor);
    double delta = 0d;
    double gamma = 0d;
    double theta = 0d;
    double divs = 0.;
    for (int i = nSteps - 1; i > -1; --i) {
      for(int j = 0; j < cashDividends.length; ++j ){
        if(i * dt < dividendTimes[j]){
          divs += cashDividends[j] * Math.exp(-interestRate * (dividendTimes[j] - i * dt));
        }
      }
      values = function.getNextOptionValuesBinomial(discount, upProbability, downProbability, values, spot,
          upFactor, downFactor, i, divs);
      if (i == 2){
        double d1 = (values.get(2) - values.get(1)) / (spot * upFactor * upFactor - spot );
        double d2 = (values.get(1) - values.get(0)) / (spot - spot * downFactor * downFactor);
        gamma = (d1 - d2) / (0.5 * spot * (upFactor * upFactor - downFactor * downFactor));
        theta = values.get(1);
      }
      if (i == 1) {
        delta = (values.get(1) - values.get(0)) / spot / (upFactor - downFactor);
      }
    }
    theta = (theta - values.get(0)) / (2 * dt) / 365;
      
    //Perform bump analysis for 
    double bumpVol = optionPrice(function, lattice, spot, volatility + BUMP, interestRate, cashDividends, dividendTimes);
    double bumpRate = optionPrice(function, lattice, spot, volatility, interestRate + BUMP, cashDividends, dividendTimes);
    double vega = (bumpVol - values.get(0)) / BUMP;
    double rho = (bumpRate - values.get(0)) / BUMP;
    return ValueDerivatives.of(values.get(0), DoubleArray.of(delta, vega, rho, theta, gamma)); 
  }
}

