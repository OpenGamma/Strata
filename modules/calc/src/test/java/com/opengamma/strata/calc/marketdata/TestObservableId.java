/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.StandardId;

/**
 * An observable ID implementation used in tests.
 */
public class TestObservableId implements ObservableId {

  private final String id;
  private final MarketDataFeed marketDataFeed;

  public TestObservableId(String id, MarketDataFeed marketDataFeed) {
    this.id = id;
    this.marketDataFeed = marketDataFeed;
  }

  @Override
  public StandardId getStandardId() {
    return StandardId.of("test", id);
  }

  @Override
  public FieldName getFieldName() {
    return FieldName.MARKET_VALUE;
  }

  @Override
  public MarketDataFeed getMarketDataFeed() {
    return marketDataFeed;
  }

  @Override
  public ObservableKey toMarketDataKey() {
    return new TestObservableKey(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestObservableId that = (TestObservableId) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(marketDataFeed, that.marketDataFeed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, marketDataFeed);
  }
}
