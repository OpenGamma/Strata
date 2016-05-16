/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * MarketDataId implementation used in tests.
 */
public class TestId implements MarketDataId<String> {

  private final String id;
  private final MarketDataFeed feed;

  public TestId(String id, MarketDataFeed feed) {
    this.id = id;
    this.feed = feed;
  }

  public TestId(String id) {
    this(id, MarketDataFeed.NONE);
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  @Override
  public MarketDataKey<String> toMarketDataKey() {
    return new TestKey(id);
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
    return Objects.equals(id, testId.id) &&
        Objects.equals(feed, testId.feed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, feed);
  }

  @Override
  public String toString() {
    return "TestId [id='" + id + "', marketDataFeed=" + feed + "]";
  }
}
