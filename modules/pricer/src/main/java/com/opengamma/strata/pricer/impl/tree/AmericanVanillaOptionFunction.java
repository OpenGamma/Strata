/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.product.common.PutCall;

public class AmericanVanillaOptionFunction implements OptionFunction{
  
  private final double strike;
  private final double timeToExpiry;
  private final double sign;
  private final int numberOfSteps;
  
  public static AmericanVanillaOptionFunction of(double strike, double timeToExpiry, PutCall putCall, int numberOfSteps) {
    double sign = putCall.isCall() ? 1d : -1d;
    ArgChecker.isTrue(numberOfSteps > 0, "the number of steps should be positive");
    return new AmericanVanillaOptionFunction(strike, timeToExpiry, sign, numberOfSteps);
  }
  
  public DoubleArray getPayoffAtExpiryTrinomial(DoubleArray stateValue) {
    int nNodes = stateValue.size();
    double[] values = new double[nNodes];
    for (int i = 0; i < nNodes; ++i) {
      values[i] = Math.max(sign * (stateValue.get(i) - strike), 0d);
    }
    return DoubleArray.ofUnsafe(values);
  }
  
  public DoubleArray getNextOptionValues(
      double discountFactor,
      DoubleMatrix transitionProbability,
      DoubleArray stateValue,
      DoubleArray values,
      int i) {
    
    int nNodes = 2 * i + 1;
    double[] res = new double[nNodes];
    for (int j = 0; j < nNodes; ++j) {
      double upProb = transitionProbability.get(j, 2);
      double middleProb = transitionProbability.get(j, 1);
      double downProb = transitionProbability.get(j, 0);
      res[j] = Math.max(sign * (stateValue.get(j) - strike),
          discountFactor * (upProb * values.get(j + 2) + middleProb * values.get(j + 1) + downProb * values.get(j)));
    }
    return DoubleArray.ofUnsafe(res);
    
  }
  
  public DoubleArray getNextOptionValuesBinomial(
      double discountFactor,
      DoubleMatrix transitionProbability,
      DoubleArray stateValue,
      DoubleArray values,
      int i) {
    
    int nNodes = i + 1;
    double[] res = new double[nNodes];
    for (int j = 0; j < nNodes; ++j) {
      double upProb = transitionProbability.get(j, 1);
      double downProb = transitionProbability.get(j, 0);
      res[j] = Math.max(sign * (stateValue.get(j) - strike),
          discountFactor * (upProb * values.get(j + 1) +downProb * values.get(j)));
    }
    return DoubleArray.ofUnsafe(res);
    
  }
  
  private AmericanVanillaOptionFunction(
      double strike,
      double timeToExpiry,
      double sign,
      int numberOfSteps) {
    this.strike = strike;
    this.timeToExpiry = timeToExpiry;
    this.sign = sign;
    this.numberOfSteps = numberOfSteps;
  }
  
  public double getStrike() {
    return strike;
  }
  public double getTimeToExpiry() {
    return timeToExpiry;
  }
  public double getSign() {
    return sign;
  }
  public int getNumberOfSteps() {
    return numberOfSteps;
  }
}
