/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * MarketDataKey implementation used in tests.
 */
public class TestKey implements MarketDataKey<String> {

  private final String value;

  public static TestKey of(String value) {
    return new TestKey(value);
  }

  public TestKey(String value) {
    this.value = value;
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestKey testId = (TestKey) o;
    return Objects.equals(value, testId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "TestId [value='" + value + "']";
  }
}
