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
 * Observable key implementation used in tests
 */
public class TestObservableKey implements ObservableKey {

  private final String id;

  public TestObservableKey(String id) {
    this.id = id;
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
  public ObservableId toMarketDataId(MarketDataFeed marketDataFeed) {
    return new TestObservableId(id, marketDataFeed);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestObservableKey that = (TestObservableKey) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
