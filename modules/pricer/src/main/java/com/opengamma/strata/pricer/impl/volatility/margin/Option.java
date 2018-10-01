package com.opengamma.strata.pricer.impl.volatility.margin;/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

public interface Option {
  double strike();  
  double expiry();
  public double notional();
  public double quantity();
  public double calculate(double spot, double rate, double vol);  
}
