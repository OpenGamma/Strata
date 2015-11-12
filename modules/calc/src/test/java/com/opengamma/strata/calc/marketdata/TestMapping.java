/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMapping;

/**
 * Market data mapping used in tests.
 */
public final class TestMapping implements MarketDataMapping<String, TestKey> {

  private final String str;

  public TestMapping(String str) {
    this.str = str;
  }

  @Override
  public Class<? extends TestKey> getMarketDataKeyType() {
    throw new UnsupportedOperationException("getMarketDataKeyType not implemented");
  }

  @Override
  public MarketDataId<String> getIdForKey(TestKey key) {
    return TestId.of(key.getValue());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestMapping that = (TestMapping) o;
    return Objects.equals(str, that.str);
  }

  @Override
  public int hashCode() {
    return Objects.hash(str);
  }

  @Override
  public String toString() {
    return "TestMapping [str='" + str + "']";
  }
}
