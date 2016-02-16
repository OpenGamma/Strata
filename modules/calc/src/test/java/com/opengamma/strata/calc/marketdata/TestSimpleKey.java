/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;

/**
 * A market data key that implements SimpleMarketDataKey and can be converted to a market data ID without
 * needing any data apart from the MarketDataFeed.
 */
public class TestSimpleKey implements SimpleMarketDataKey<String> {

  private final String id;

  public TestSimpleKey(String id) {
    this.id = id;
  }

  @Override
  public MarketDataId<String> toMarketDataId(MarketDataFeed marketDataFeed) {
    return new TestSimpleId(id, marketDataFeed);
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
    TestSimpleKey that = (TestSimpleKey) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
