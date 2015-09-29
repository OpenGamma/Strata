/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.Objects;

import com.opengamma.strata.collect.id.StandardId;

/**
 * ObservableKey implementation used in tests.
 */
public class TestObservableKey implements ObservableKey {

  private final StandardId id;

  private final FieldName fieldName;

  public static TestObservableKey of(String id) {
    return new TestObservableKey(id, FieldName.MARKET_VALUE);
  }
  public static TestObservableKey of(StandardId id) {
    return new TestObservableKey(id, FieldName.MARKET_VALUE);
  }

  TestObservableKey(String id, FieldName fieldName) {
    this(StandardId.of("test", id), fieldName);
  }

  TestObservableKey(StandardId id, FieldName fieldName) {
    this.id = id;
    this.fieldName = fieldName;
  }

  @Override
  public StandardId getStandardId() {
    return id;
  }

  @Override
  public FieldName getFieldName() {
    return fieldName;
  }

  @Override
  public ObservableId toObservableId(MarketDataFeed marketDataFeed) {
    return new TestObservableId(id, marketDataFeed);
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
    TestObservableKey that = (TestObservableKey) obj;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TestObservableId [id=" + id + ", field=" + fieldName + "]";
  }

}
