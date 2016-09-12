/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

/**
 * MarketDataName implementation used in tests.
 */
public class TestingName2 extends MarketDataName<String> {

  private final String name;

  public TestingName2(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

}
