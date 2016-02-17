/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * A test market data ID.
 */
public class TestSimpleId implements MarketDataId<String> {

  private final String id;
  private final MarketDataFeed marketDataFeed;

  public TestSimpleId(String id, MarketDataFeed marketDataFeed) {
    this.id = id;
    this.marketDataFeed = marketDataFeed;
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  @Override
  public MarketDataKey<String> toMarketDataKey() {
    throw new UnsupportedOperationException("toMarketDataKey not implemented");
  }

  public MarketDataFeed getMarketDataFeed() {
    return marketDataFeed;
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
        Objects.equals(marketDataFeed, that.marketDataFeed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, marketDataFeed);
  }
}
