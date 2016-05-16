/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;

/**
 * A test market data ID.
 */
public class TestSimpleId implements MarketDataId<String> {

  private final String id;
  private final MarketDataFeed feed;

  public TestSimpleId(String id, MarketDataFeed feed) {
    this.id = id;
    this.feed = feed;
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  public MarketDataFeed getMarketDataFeed() {
    return feed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestSimpleId that = (TestSimpleId) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(feed, that.feed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, feed);
  }
}
