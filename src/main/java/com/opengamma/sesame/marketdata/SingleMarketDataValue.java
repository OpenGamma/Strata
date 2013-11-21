/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

// TODO should this be parameterized rather than assuming a double value? or change name?
// TODO and have a different type for a generic value
public class SingleMarketDataValue implements MarketDataValue<Double> {

  private final Double value;

  public SingleMarketDataValue(Double value) {
    this.value = value;
  }

  @Override
  public Double getValue() {
    return value;
  }
}
