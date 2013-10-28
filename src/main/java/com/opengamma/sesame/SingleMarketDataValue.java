/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

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
