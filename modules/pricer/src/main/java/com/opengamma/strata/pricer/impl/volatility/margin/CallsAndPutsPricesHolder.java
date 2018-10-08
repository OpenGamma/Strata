/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

public class CallsAndPutsPricesHolder {
  private double[] strikes;
  private double[] calls;
  private double[] puts;
  private double daysToExpiry;
  
  CallsAndPutsPricesHolder(double[][] optionsData, double daysToExpiry){
    this.strikes = optionsData[0];
    this.calls = optionsData[1];
    this.puts = optionsData[2];
    this.daysToExpiry = daysToExpiry;
  }
  
  public double[] getStrikes(){
    return strikes;
  }
  
  public double[] getCalls(){
    return calls;
  }
  
  public double[] getPuts(){
    return puts;
  }
  
  public double daysToExpiry(){
    return daysToExpiry;
  }
}
