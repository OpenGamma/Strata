/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMapping;

/**
 * Market data mapping used in tests.
 */
public final class TestMapping implements MarketDataMapping<String, TestKey> {

  private final String str;
  private final MarketDataFeed marketDataFeed;

  public TestMapping(String str, MarketDataFeed marketDataFeed) {
    this.str = str;
    this.marketDataFeed = marketDataFeed;
  }

  public TestMapping(String str) {
    this(str, MarketDataFeed.NONE);
  }

  @Override
  public Class<? extends TestKey> getMarketDataKeyType() {
    return TestKey.class;
  }

  @Override
  public MarketDataId<String> getIdForKey(TestKey key) {
    return new TestId(key.getId(), marketDataFeed);
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
    return Objects.equals(str, that.str) &&
        Objects.equals(marketDataFeed, that.marketDataFeed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(str, marketDataFeed);
  }

  @Override
  public String toString() {
    return "TestMapping [str='" + str + "', marketDataFeed=" + marketDataFeed + "]";
  }
}
