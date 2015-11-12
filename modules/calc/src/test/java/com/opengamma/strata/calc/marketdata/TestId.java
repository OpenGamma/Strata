/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataId;

/**
 * MarketDataId implementation used in tests.
 */
public class TestId implements MarketDataId<String> {

  private final String value;

  public static TestId of(String value) {
    return new TestId(value);
  }

  public TestId(String value) {
    this.value = value;
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestId testId = (TestId) o;
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
