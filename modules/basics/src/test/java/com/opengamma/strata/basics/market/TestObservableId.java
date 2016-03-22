/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.Objects;

/**
 * ObservableId implementation used in tests.
 */
public class TestObservableId implements ObservableId, ReferenceDataId<Number> {

  private final StandardId id;

  private final MarketDataFeed feed;

  public static TestObservableId of(String id) {
    return new TestObservableId(id, MarketDataFeed.NONE);
  }

  public static TestObservableId of(String id, MarketDataFeed feed) {
    return new TestObservableId(id, feed);
  }

  public static TestObservableId of(StandardId id) {
    return new TestObservableId(id, MarketDataFeed.NONE);
  }

  public static TestObservableId of(StandardId id, MarketDataFeed feed) {
    return new TestObservableId(id, feed);
  }

  TestObservableId(String id, MarketDataFeed feed) {
    this(StandardId.of("test", id), feed);
  }

  TestObservableId(StandardId id, MarketDataFeed feed) {
    this.feed = feed;
    this.id = id;
  }

  @Override
  public StandardId getStandardId() {
    return id;
  }

  @Override
  public FieldName getFieldName() {
    return FieldName.MARKET_VALUE;
  }

  @Override
  public MarketDataFeed getMarketDataFeed() {
    return feed;
  }

  @Override
  public ObservableKey toMarketDataKey() {
    return TestObservableKey.of(id);
  }

  @Override
  public Class<Number> getReferenceDataType() {
    return Number.class;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TestObservableId that = (TestObservableId) obj;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TestObservableId [id=" + id + ", feed=" + feed + "]";
  }
}
